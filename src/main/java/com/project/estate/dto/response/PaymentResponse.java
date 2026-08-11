package com.project.estate.dto.response;

import com.project.estate.enums.PaymentMethod;
import com.project.estate.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private String id;
    private String reservationId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionRef;
    private String idempotencyKey;
    private String checkoutUrl;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
