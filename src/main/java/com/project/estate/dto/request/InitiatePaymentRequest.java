package com.project.estate.dto.request;

import com.project.estate.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record InitiatePaymentRequest(
    @NotBlank(message = "Reservation ID is required") String reservationId,
    @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
    BigDecimal amount) {}
