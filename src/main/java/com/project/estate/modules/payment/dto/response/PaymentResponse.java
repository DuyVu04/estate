package com.project.estate.modules.payment.dto.response;

import com.project.estate.modules.payment.enums.PaymentMethod;
import com.project.estate.modules.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentResponse(
    String id,
    String reservationId,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    String transactionRef,
    String idempotencyKey,
    String checkoutUrl,
    LocalDateTime paidAt,
    LocalDateTime createdAt) {}
