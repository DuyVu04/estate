package com.project.estate.service;

import com.project.estate.dto.request.InitiatePaymentRequest;
import com.project.estate.dto.request.PaymentWebhookRequest;
import com.project.estate.dto.response.PaymentResponse;
import com.project.estate.entity.Payment;
import com.project.estate.entity.Reservation;
import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.PaymentStatus;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.event.DepositPaidEvent;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.PaymentMapper;
import com.project.estate.repository.PaymentRepository;
import com.project.estate.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final ReservationRepository reservationRepository;
  private final PaymentMapper paymentMapper;
  private final ReservationService reservationService;
  private final ApplicationEventPublisher eventPublisher;
  private final com.project.estate.messaging.producer.EmailProducer emailProducer;

  @Transactional
  public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
    Reservation reservation =
        reservationRepository
            .findById(request.reservationId())
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    if (reservation.getStatus() != ReservationStatus.ACTIVE) {
      throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
    }

    String idempotencyKey =
        "PAY-RES-" + reservation.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
    BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.valueOf(50000000);

    Payment payment =
        Payment.builder()
            .reservation(reservation)
            .amount(amount)
            .paymentMethod(request.paymentMethod())
            .status(PaymentStatus.PENDING)
            .idempotencyKey(idempotencyKey)
            .build();

    paymentRepository.save(payment);
    log.info(
        "[PAYMENT_SERVICE] Payment initiated: id={}, reservationId={}, amount={}",
        payment.getId(),
        reservation.getId(),
        amount);

    PaymentResponse baseResponse = paymentMapper.toResponse(payment);
    return PaymentResponse.builder()
        .id(baseResponse.id())
        .reservationId(baseResponse.reservationId())
        .amount(baseResponse.amount())
        .paymentMethod(baseResponse.paymentMethod())
        .status(baseResponse.status())
        .transactionRef(baseResponse.transactionRef())
        .idempotencyKey(baseResponse.idempotencyKey())
        .checkoutUrl(
            "http://localhost:8080/api/v1/payments/mock-checkout?paymentId=" + payment.getId())
        .paidAt(baseResponse.paidAt())
        .createdAt(baseResponse.createdAt())
        .build();
  }

  public PaymentResponse processWebhook(PaymentWebhookRequest request) {
    log.info(
        "[PAYMENT_WEBHOOK] Received webhook for reservationId={}, idempotencyKey={}",
        request.reservationId(),
        request.idempotencyKey());

    // 1. Idempotency Check
    var existingOpt = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
    if (existingOpt.isPresent() && existingOpt.get().getStatus() == PaymentStatus.SUCCESS) {
      log.warn(
          "[PAYMENT_WEBHOOK] Duplicate webhook detected for idempotencyKey={}.",
          request.idempotencyKey());
      return paymentMapper.toResponse(existingOpt.get());
    }

    // 2. Save Payment record in its own transaction (committed before payDeposit)
    Payment payment = savePaymentFromWebhook(request);

    // 3. Trigger Workflow Engine in a SEPARATE transaction (AOP + REQUIRES_NEW works correctly)
    reservationService.payDeposit(payment.getReservation().getId());

    // 4. Publish email event
    Reservation reservation = payment.getReservation();
    if (reservation.getUser() != null && reservation.getUser().getEmail() != null) {
      String propertyTitle =
          reservation.getProperty() != null ? reservation.getProperty().getTitle() : "Bất động sản";

      // Local Spring Event (In-memory)
      eventPublisher.publishEvent(
          new DepositPaidEvent(
              this,
              reservation.getId(),
              reservation.getUser().getEmail(),
              propertyTitle,
              request.amount(),
              request.transactionRef()));

      // Distributed RabbitMQ Message
      emailProducer.sendDepositPaid(
          new com.project.estate.messaging.dto.DepositPaidMessage(
              reservation.getId(),
              reservation.getUser().getEmail(),
              propertyTitle,
              request.amount(),
              request.transactionRef()));
    }

    log.info(
        "[PAYMENT_WEBHOOK] Webhook processed successfully. Reservation status updated to DEPOSIT_PAID.");
    return paymentMapper.toResponse(payment);
  }

  @Transactional
  public Payment savePaymentFromWebhook(PaymentWebhookRequest request) {
    Reservation reservation =
        reservationRepository
            .findById(request.reservationId())
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    var existingOpt = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
    Payment payment =
        existingOpt.orElseGet(
            () ->
                Payment.builder()
                    .reservation(reservation)
                    .idempotencyKey(request.idempotencyKey())
                    .build());

    payment.setAmount(request.amount());
    payment.setPaymentMethod(request.paymentMethod());
    payment.setStatus(PaymentStatus.SUCCESS);
    payment.setTransactionRef(request.transactionRef());
    payment.setPaidAt(LocalDateTime.now());

    return paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public PaymentResponse getPaymentById(String id) {
    Payment payment =
        paymentRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    return paymentMapper.toResponse(payment);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getPaymentsByReservation(String reservationId) {
    return paymentRepository.findByReservationId(reservationId).stream()
        .map(paymentMapper::toResponse)
        .toList();
  }
}
