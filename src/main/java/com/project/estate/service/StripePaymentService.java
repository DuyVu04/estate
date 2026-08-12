package com.project.estate.service;

import com.project.estate.config.StripeConfig;
import com.project.estate.dto.request.InitiatePaymentRequest;
import com.project.estate.dto.response.PaymentResponse;
import com.project.estate.entity.Payment;
import com.project.estate.entity.Reservation;
import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.PaymentMethod;
import com.project.estate.enums.PaymentStatus;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.event.DepositPaidEvent;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.PaymentMapper;
import com.project.estate.repository.PaymentRepository;
import com.project.estate.repository.ReservationRepository;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for Stripe Checkout Session creation and Webhook processing.
 *
 * <p>Idempotency is guaranteed at 3 levels: 1. Stripe SDK: IdempotencyKey sent with every create()
 * call prevents duplicate sessions. 2. DB: Unique constraint on payments.idempotency_key prevents
 * duplicate payment records. 3. Webhook: Checks existing Payment status before processing, returns
 * early if already SUCCESS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentService {

  private final StripeClient stripeClient;
  private final StripeConfig stripeConfig;
  private final PaymentRepository paymentRepository;
  private final ReservationRepository reservationRepository;
  private final ReservationService reservationService;
  private final PaymentMapper paymentMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final com.project.estate.messaging.producer.EmailProducer emailProducer;

  /**
   * Creates a Stripe Checkout Session for the given reservation. The reservation's depositAmount is
   * used as the payment amount.
   *
   * <p>Idempotency: Uses "STRIPE-RES-{reservationId}" as both the DB idempotencyKey and the Stripe
   * API IdempotencyKey, ensuring that retrying this call for the same reservation returns the same
   * Stripe Session.
   */
  @Transactional
  public PaymentResponse createCheckoutSession(InitiatePaymentRequest request) {
    // 1. Validate Reservation
    Reservation reservation =
        reservationRepository
            .findById(request.getReservationId())
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    if (reservation.getStatus() != ReservationStatus.ACTIVE) {
      throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
    }

    // 2. Idempotency: Check if a payment already exists for this reservation
    String idempotencyKey = "STRIPE-RES-" + reservation.getId();
    var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existingPayment.isPresent()) {
      log.info(
          "[STRIPE] Returning existing checkout session for idempotencyKey={}", idempotencyKey);
      PaymentResponse response = paymentMapper.toResponse(existingPayment.get());
      response.setCheckoutUrl(
          existingPayment.get().getTransactionRef() != null
              ? retrieveSessionUrl(existingPayment.get().getTransactionRef())
              : null);
      return response;
    }

    // 3. Determine amount (use depositAmount from reservation, fallback to request amount)
    BigDecimal amount =
        reservation.getDepositAmount() != null
            ? reservation.getDepositAmount()
            : (request.getAmount() != null ? request.getAmount() : BigDecimal.valueOf(50000000));

    // 4. Build Stripe Checkout Session
    String propertyTitle =
        reservation.getProperty() != null
            ? reservation.getProperty().getTitle()
            : "Đặt cọc Bất động sản";

    try {
      SessionCreateParams params =
          SessionCreateParams.builder()
              .setMode(SessionCreateParams.Mode.PAYMENT)
              .setSuccessUrl(stripeConfig.getSuccessUrl())
              .setCancelUrl(stripeConfig.getCancelUrl())
              .setClientReferenceId(reservation.getId())
              .putMetadata("reservationId", reservation.getId())
              .putMetadata("idempotencyKey", idempotencyKey)
              .addLineItem(
                  SessionCreateParams.LineItem.builder()
                      .setQuantity(1L)
                      .setPriceData(
                          SessionCreateParams.LineItem.PriceData.builder()
                              .setCurrency("vnd")
                              .setUnitAmount(amount.longValue())
                              .setProductData(
                                  SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                      .setName("Đặt cọc: " + propertyTitle)
                                      .setDescription(
                                          "Tiền cọc giữ chỗ bất động sản - Mã: "
                                              + reservation.getId())
                                      .build())
                              .build())
                      .build())
              .build();

      // 5. Call Stripe API with IdempotencyKey
      Session session =
          stripeClient
              .checkout()
              .sessions()
              .create(
                  params,
                  com.stripe.net.RequestOptions.builder()
                      .setIdempotencyKey(idempotencyKey)
                      .build());

      // 6. Persist Payment record
      Payment payment =
          Payment.builder()
              .reservation(reservation)
              .amount(amount)
              .paymentMethod(PaymentMethod.STRIPE)
              .status(PaymentStatus.PENDING)
              .idempotencyKey(idempotencyKey)
              .transactionRef(session.getId())
              .build();

      paymentRepository.save(payment);

      log.info(
          "[STRIPE] Checkout session created: sessionId={}, reservationId={}, amount={}",
          session.getId(),
          reservation.getId(),
          amount);

      PaymentResponse response = paymentMapper.toResponse(payment);
      response.setCheckoutUrl(session.getUrl());
      return response;

    } catch (StripeException e) {
      log.error("[STRIPE] Failed to create checkout session: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Processes incoming Stripe Webhook events. Handles "checkout.session.completed" to transition
   * reservation to DEPOSIT_PAID.
   *
   * <p>Idempotency is guaranteed by checking the Payment status before processing. If payment is
   * already SUCCESS, returns immediately without re-processing.
   */
  public void processWebhook(String payload, String sigHeader) {
    // 1. Verify Stripe Signature
    Event event;
    try {
      event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    } catch (SignatureVerificationException e) {
      log.error("[STRIPE_WEBHOOK] Invalid signature: {}", e.getMessage());
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    } catch (Exception e) {
      log.error("[STRIPE_WEBHOOK] Failed to parse webhook payload: {}", e.getMessage());
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    log.info("[STRIPE_WEBHOOK] Received event: type={}, id={}", event.getType(), event.getId());

    // 2. Only handle checkout.session.completed
    if (!"checkout.session.completed".equals(event.getType())) {
      log.info("[STRIPE_WEBHOOK] Ignoring event type: {}", event.getType());
      return;
    }

    // 3. Deserialize Session from event
    Session session =
        (Session)
            event
                .getDataObjectDeserializer()
                .getObject()
                .orElseThrow(
                    () -> {
                      log.error("[STRIPE_WEBHOOK] Failed to deserialize session from event");
                      return new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
                    });

    String reservationId = session.getClientReferenceId();
    if (reservationId == null) {
      reservationId = session.getMetadata().get("reservationId");
    }

    log.info(
        "[STRIPE_WEBHOOK] Processing checkout.session.completed: sessionId={}, reservationId={}",
        session.getId(),
        reservationId);

    // 4. Idempotency Check: Find payment by Stripe Session ID (transactionRef)
    String idempotencyKey =
        session.getMetadata() != null
            ? session.getMetadata().getOrDefault("idempotencyKey", "STRIPE-RES-" + reservationId)
            : "STRIPE-RES-" + reservationId;

    var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
      log.warn(
          "[STRIPE_WEBHOOK] Duplicate event detected for idempotencyKey={}. Skipping.",
          idempotencyKey);
      return;
    }

    // 5. Update Payment record
    updatePaymentToSuccess(idempotencyKey, session.getId(), reservationId);

    // 6. Trigger Workflow Engine (separate transaction)
    reservationService.payDeposit(reservationId);

    // 7. Publish Email Event
    publishDepositPaidEvent(reservationId, session);

    log.info(
        "[STRIPE_WEBHOOK] Successfully processed. Reservation {} -> DEPOSIT_PAID", reservationId);
  }

  /**
   * Updates Payment record status to SUCCESS. Runs in its own transaction so it commits BEFORE
   * payDeposit() is called.
   */
  @Transactional
  public void updatePaymentToSuccess(
      String idempotencyKey, String sessionId, String reservationId) {
    var paymentOpt = paymentRepository.findByIdempotencyKey(idempotencyKey);

    Payment payment;
    if (paymentOpt.isPresent()) {
      payment = paymentOpt.get();
    } else {
      // Edge case: webhook arrived before initiate completed (rare but possible)
      Reservation reservation =
          reservationRepository
              .findById(reservationId)
              .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
      payment =
          Payment.builder()
              .reservation(reservation)
              .amount(
                  reservation.getDepositAmount() != null
                      ? reservation.getDepositAmount()
                      : BigDecimal.valueOf(50000000))
              .paymentMethod(PaymentMethod.STRIPE)
              .idempotencyKey(idempotencyKey)
              .build();
    }

    payment.setStatus(PaymentStatus.SUCCESS);
    payment.setTransactionRef(sessionId);
    payment.setPaidAt(LocalDateTime.now());
    paymentRepository.save(payment);
  }

  private void publishDepositPaidEvent(String reservationId, Session session) {
    Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
    if (reservation == null
        || reservation.getUser() == null
        || reservation.getUser().getEmail() == null) {
      return;
    }

    String propertyTitle =
        reservation.getProperty() != null ? reservation.getProperty().getTitle() : "Bất động sản";

    BigDecimal amount =
        reservation.getDepositAmount() != null ? reservation.getDepositAmount() : BigDecimal.ZERO;

    // 1. Local Spring Event (In-memory)
    eventPublisher.publishEvent(
        new DepositPaidEvent(
            this,
            reservationId,
            reservation.getUser().getEmail(),
            propertyTitle,
            amount,
            session.getId()));

    // 2. Distributed RabbitMQ Message (Persistent + DLQ via messaging package)
    emailProducer.sendDepositPaid(
        new com.project.estate.messaging.dto.DepositPaidMessage(
            reservationId,
            reservation.getUser().getEmail(),
            propertyTitle,
            amount,
            session.getId()));
  }

  /**
   * Retrieves the checkout URL for an existing Stripe session. Used when returning an idempotent
   * response for a duplicate initiate request.
   */
  private String retrieveSessionUrl(String sessionId) {
    try {
      Session session = stripeClient.checkout().sessions().retrieve(sessionId);
      return session.getUrl();
    } catch (StripeException e) {
      log.warn(
          "[STRIPE] Could not retrieve session URL for sessionId={}: {}",
          sessionId,
          e.getMessage());
      return null;
    }
  }

  /**
   * Fallback Confirm API — Called by Frontend after Stripe redirects to success page.
   *
   * <p>Use case: If webhook is delayed or fails, Frontend proactively confirms payment by providing
   * the Stripe sessionId. Backend calls Stripe API to verify payment status.
   *
   * <p>Idempotency: If webhook already processed this payment (status == SUCCESS), this method
   * returns the existing PaymentResponse without re-processing.
   *
   * @param sessionId The Stripe Checkout Session ID (cs_test_...)
   * @return PaymentResponse with current payment status
   */
  public PaymentResponse confirmPayment(String sessionId) {
    log.info("[STRIPE_CONFIRM] Confirming payment for sessionId={}", sessionId);

    // 1. Call Stripe API to retrieve session status
    Session session;
    try {
      session = stripeClient.checkout().sessions().retrieve(sessionId);
    } catch (StripeException e) {
      log.error("[STRIPE_CONFIRM] Failed to retrieve session: {}", e.getMessage());
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 2. Extract reservationId from session metadata
    String reservationId = session.getClientReferenceId();
    if (reservationId == null && session.getMetadata() != null) {
      reservationId = session.getMetadata().get("reservationId");
    }

    if (reservationId == null) {
      log.error(
          "[STRIPE_CONFIRM] No reservationId found in session metadata for sessionId={}",
          sessionId);
      throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    String idempotencyKey =
        session.getMetadata() != null
            ? session.getMetadata().getOrDefault("idempotencyKey", "STRIPE-RES-" + reservationId)
            : "STRIPE-RES-" + reservationId;

    // 3. Idempotency Check: If already processed by webhook, return existing result
    var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
      log.info(
          "[STRIPE_CONFIRM] Payment already confirmed (likely by webhook). Returning existing response.");
      return paymentMapper.toResponse(existingPayment.get());
    }

    // 4. Verify Stripe says payment is actually paid
    if (!"paid".equals(session.getPaymentStatus())) {
      log.warn(
          "[STRIPE_CONFIRM] Session {} payment_status is '{}', not 'paid'. Returning PENDING.",
          sessionId,
          session.getPaymentStatus());

      if (existingPayment.isPresent()) {
        return paymentMapper.toResponse(existingPayment.get());
      }
      throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
    }

    // 5. Payment is confirmed — process exactly like webhook
    log.info(
        "[STRIPE_CONFIRM] Payment verified as 'paid'. Processing confirmation for reservationId={}",
        reservationId);

    updatePaymentToSuccess(idempotencyKey, sessionId, reservationId);
    reservationService.payDeposit(reservationId);
    publishDepositPaidEvent(reservationId, session);

    // 6. Return updated response
    Payment payment =
        paymentRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

    log.info(
        "[STRIPE_CONFIRM] Successfully confirmed. Reservation {} -> DEPOSIT_PAID", reservationId);
    return paymentMapper.toResponse(payment);
  }
}
