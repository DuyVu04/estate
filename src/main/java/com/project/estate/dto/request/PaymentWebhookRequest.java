package com.project.estate.dto.request;

import com.project.estate.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PaymentWebhookRequest(
    @NotBlank(message = "Reservation ID is required") String reservationId,
    @NotNull(message = "Amount is required") BigDecimal amount,
    @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
    @NotBlank(message = "Transaction reference is required") String transactionRef,
    @NotBlank(message = "Idempotency key is required") String idempotencyKey) {}
