package com.project.estate.dto.request;

import com.project.estate.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentRequest {

  @NotBlank(message = "Reservation ID is required")
  private String reservationId;

  @NotNull(message = "Payment method is required")
  private PaymentMethod paymentMethod;

  private BigDecimal amount;
}
