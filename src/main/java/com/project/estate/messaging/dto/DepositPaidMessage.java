package com.project.estate.messaging.dto;

import java.math.BigDecimal;

/**
 * Message DTO for Deposit Paid notification sent via RabbitMQ. Includes retryCount and nextRetry()
 * helper for the codebase's retry/DLQ pipeline.
 */
public record DepositPaidMessage(
    String reservationId,
    String userEmail,
    String propertyTitle,
    BigDecimal amount,
    String transactionRef,
    int retryCount) {
  public DepositPaidMessage(
      String reservationId,
      String userEmail,
      String propertyTitle,
      BigDecimal amount,
      String transactionRef) {
    this(reservationId, userEmail, propertyTitle, amount, transactionRef, 0);
  }

  public DepositPaidMessage nextRetry() {
    return new DepositPaidMessage(
        reservationId, userEmail, propertyTitle, amount, transactionRef, retryCount + 1);
  }
}
