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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.project.estate.messaging.producer.EmailProducer emailProducer;

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        String idempotencyKey = "PAY-RES-" + reservation.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.valueOf(50000000);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .amount(amount)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        paymentRepository.save(payment);
        log.info("[PAYMENT_SERVICE] Payment initiated: id={}, reservationId={}, amount={}",
                payment.getId(), reservation.getId(), amount);

        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setCheckoutUrl("http://localhost:8080/api/v1/payments/mock-checkout?paymentId=" + payment.getId());
        return response;
    }

    /**
     * Orchestrator method — intentionally NOT @Transactional.
     * Step 1 (savePaymentFromWebhook) commits Payment record first.
     * Step 2 (payDeposit) runs in its own transaction so WorkflowEngine AOP works correctly.
     * Step 3 (publishEvent) fires email notification after all DB writes are committed.
     */
    public PaymentResponse processWebhook(PaymentWebhookRequest request) {
        log.info("[PAYMENT_WEBHOOK] Received webhook for reservationId={}, idempotencyKey={}",
                request.getReservationId(), request.getIdempotencyKey());

        // 1. Idempotency Check
        var existingOpt = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == PaymentStatus.SUCCESS) {
            log.warn("[PAYMENT_WEBHOOK] Duplicate webhook detected for idempotencyKey={}.",
                    request.getIdempotencyKey());
            return paymentMapper.toResponse(existingOpt.get());
        }

        // 2. Save Payment record in its own transaction (committed before payDeposit)
        Payment payment = savePaymentFromWebhook(request);

        // 3. Trigger Workflow Engine in a SEPARATE transaction (AOP + REQUIRES_NEW works correctly)
        reservationService.payDeposit(payment.getReservation().getId());

        // 4. Publish email event (runs async after commit thanks to @TransactionalEventListener AFTER_COMMIT)
        // 5. Bắn Message lên RabbitMQ Queue để xử lý gửi Email phân tán (Distributed Event-Driven)
        Reservation reservation = payment.getReservation();
        if (reservation.getUser() != null && reservation.getUser().getEmail() != null) {
            String propertyTitle = reservation.getProperty() != null ? reservation.getProperty().getTitle() : "Bất động sản";
            
            // Local Spring Event (In-memory)
            eventPublisher.publishEvent(new DepositPaidEvent(
                    this,
                    reservation.getId(),
                    reservation.getUser().getEmail(),
                    propertyTitle,
                    request.getAmount(),
                    request.getTransactionRef()
            ));

            // Distributed RabbitMQ Message (Persistent + DLQ via messaging package)
            emailProducer.sendDepositPaid(new com.project.estate.messaging.dto.DepositPaidMessage(
                    reservation.getId(),
                    reservation.getUser().getEmail(),
                    propertyTitle,
                    request.getAmount(),
                    request.getTransactionRef()
            ));
        }

        log.info("[PAYMENT_WEBHOOK] Webhook processed successfully. Reservation status updated to DEPOSIT_PAID.");
        return paymentMapper.toResponse(payment);
    }

    /**
     * Saves or updates Payment record from webhook data.
     * Runs in its own transaction so the Payment row is committed BEFORE payDeposit() is called.
     */
    @Transactional
    public Payment savePaymentFromWebhook(PaymentWebhookRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        var existingOpt = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        Payment payment = existingOpt.orElseGet(() -> Payment.builder()
                .reservation(reservation)
                .idempotencyKey(request.getIdempotencyKey())
                .build());

        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionRef(request.getTransactionRef());
        payment.setPaidAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByReservation(String reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }
}

