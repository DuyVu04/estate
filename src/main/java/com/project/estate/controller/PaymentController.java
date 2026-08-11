package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.InitiatePaymentRequest;
import com.project.estate.dto.request.PaymentWebhookRequest;
import com.project.estate.dto.response.PaymentResponse;
import com.project.estate.service.PaymentService;
import com.project.estate.service.StripePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;

    // ==========================================
    // Mock Payment Endpoints (Giả lập)
    // ==========================================

    @PostMapping("/initiate")
    public ApiResponse<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.success(paymentService.initiatePayment(request));
    }

    @PostMapping("/webhook")
    public ApiResponse<PaymentResponse> processWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        return ApiResponse.success(paymentService.processWebhook(request));
    }

    // ==========================================
    // Stripe Payment Endpoints (Cổng thanh toán thật)
    // ==========================================

    /**
     * Creates a Stripe Checkout Session for the given reservation.
     * Returns a checkoutUrl that the Frontend redirects the customer to.
     */
    @PostMapping("/stripe/initiate")
    public ApiResponse<PaymentResponse> stripeInitiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.success(stripePaymentService.createCheckoutSession(request));
    }

    /**
     * Receives Stripe Webhook events.
     * Stripe sends the raw JSON payload with a Stripe-Signature header for verification.
     * This endpoint must accept raw String body (not parsed JSON) for signature verification to work.
     */
    @PostMapping("/webhook/stripe")
    public ApiResponse<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        stripePaymentService.processWebhook(payload, sigHeader);
        return ApiResponse.success(null);
    }

    /**
     * Fallback Confirm Endpoint — Called by Frontend after Stripe redirects to success page.
     * Accepts session_id or sessionId via Query Parameter (?session_id=... or ?sessionId=...).
     */
    @PostMapping("/stripe/confirm")
    public ApiResponse<PaymentResponse> stripeConfirm(
            @RequestParam(value = "sessionId", required = false) String camelSessionId,
            @RequestParam(value = "session_id", required = false) String snakeSessionId
    ) {
        String sessionId = (camelSessionId != null && !camelSessionId.isBlank())
                ? camelSessionId
                : snakeSessionId;

        if (sessionId == null || sessionId.isBlank()) {
            throw new com.project.estate.exception.AppException(com.project.estate.enums.ErrorCode.INVALID_REQUEST);
        }

        return ApiResponse.success(stripePaymentService.confirmPayment(sessionId));
    }

    // ==========================================
    // Common Query Endpoints
    // ==========================================

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPaymentById(@PathVariable String id) {
        return ApiResponse.success(paymentService.getPaymentById(id));
    }

    @GetMapping("/reservation/{reservationId}")
    public ApiResponse<List<PaymentResponse>> getPaymentsByReservation(@PathVariable String reservationId) {
        return ApiResponse.success(paymentService.getPaymentsByReservation(reservationId));
    }
}
