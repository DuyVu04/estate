package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.InitiatePaymentRequest;
import com.project.estate.dto.request.PaymentWebhookRequest;
import com.project.estate.dto.response.PaymentResponse;
import com.project.estate.service.PaymentService;
import com.project.estate.service.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(
    name = "Payments & Stripe Gateway",
    description =
        "Endpoints for processing reservation deposit payments, Stripe Checkout Sessions, and webhooks")
public class PaymentController {

  private final PaymentService paymentService;
  private final StripePaymentService stripePaymentService;

  // ==========================================
  // Mock Payment Endpoints (Giả lập)
  // ==========================================

  @PostMapping("/initiate")
  @Operation(
      summary = "Initiate mock payment",
      description = "Simulates payment initiation for reservation deposit testing")
  public ApiResponse<PaymentResponse> initiatePayment(
      @Valid @RequestBody InitiatePaymentRequest request) {
    return ApiResponse.success(paymentService.initiatePayment(request));
  }

  @PostMapping("/webhook")
  @Operation(
      summary = "Process mock payment webhook",
      description = "Simulates incoming payment gateway callback")
  public ApiResponse<PaymentResponse> processWebhook(
      @Valid @RequestBody PaymentWebhookRequest request) {
    return ApiResponse.success(paymentService.processWebhook(request));
  }

  // ==========================================
  // Stripe Payment Endpoints (Cổng thanh toán thật)
  // ==========================================

  @PostMapping("/stripe/initiate")
  @Operation(
      summary = "Create Stripe Checkout Session",
      description =
          "Creates a real Stripe Hosted Checkout Session for the reservation deposit and returns the redirect URL")
  public ApiResponse<PaymentResponse> stripeInitiate(
      @Valid @RequestBody InitiatePaymentRequest request) {
    return ApiResponse.success(stripePaymentService.createCheckoutSession(request));
  }

  @PostMapping("/webhook/stripe")
  @Operation(
      summary = "Handle Stripe Webhook",
      description =
          "Secure webhook receiver for asynchronous Stripe events (checkout.session.completed, etc.) verified via Stripe-Signature")
  public ApiResponse<Void> stripeWebhook(
      @RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
    stripePaymentService.processWebhook(payload, sigHeader);
    return ApiResponse.success(null);
  }

  @PostMapping("/stripe/confirm")
  @Operation(
      summary = "Confirm Stripe Checkout Session",
      description =
          "Fallback endpoint called when customer is redirected back to success page with session_id")
  public ApiResponse<PaymentResponse> stripeConfirm(
      @RequestParam(value = "sessionId", required = false) String camelSessionId,
      @RequestParam(value = "session_id", required = false) String snakeSessionId) {
    String sessionId =
        (camelSessionId != null && !camelSessionId.isBlank()) ? camelSessionId : snakeSessionId;

    if (sessionId == null || sessionId.isBlank()) {
      throw new com.project.estate.exception.AppException(
          com.project.estate.enums.ErrorCode.INVALID_REQUEST);
    }

    return ApiResponse.success(stripePaymentService.confirmPayment(sessionId));
  }

  // ==========================================
  // Common Query Endpoints
  // ==========================================

  @GetMapping("/{id}")
  @Operation(
      summary = "Get payment by ID",
      description = "Retrieves payment details and status by payment UUID")
  public ApiResponse<PaymentResponse> getPaymentById(@PathVariable String id) {
    return ApiResponse.success(paymentService.getPaymentById(id));
  }

  @GetMapping("/reservation/{reservationId}")
  @Operation(
      summary = "Get payments by reservation ID",
      description = "Retrieves all payment transactions associated with a specific reservation")
  public ApiResponse<List<PaymentResponse>> getPaymentsByReservation(
      @PathVariable String reservationId) {
    return ApiResponse.success(paymentService.getPaymentsByReservation(reservationId));
  }
}
