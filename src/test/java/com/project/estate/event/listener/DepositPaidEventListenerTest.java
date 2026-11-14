package com.project.estate.event.listener;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.event.DepositPaidEvent;
import com.project.estate.service.email.EmailTemplateService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DepositPaidEventListenerTest {

  static class StubEmailTemplateService extends EmailTemplateService {
    String sentToEmail;
    String sentReservationId;
    String sentPropertyTitle;
    BigDecimal sentAmount;
    String sentTransactionId;
    int callCount = 0;

    public StubEmailTemplateService() {
      super(null, null);
    }

    @Override
    public void sendDepositPaidEmail(
        String toEmail,
        String reservationId,
        String propertyTitle,
        BigDecimal amount,
        String transactionId) {
      callCount++;
      this.sentToEmail = toEmail;
      this.sentReservationId = reservationId;
      this.sentPropertyTitle = propertyTitle;
      this.sentAmount = amount;
      this.sentTransactionId = transactionId;
    }
  }

  @Test
  @DisplayName("Should send deposit paid email when DepositPaidEvent is received")
  void handleDepositPaidEvent_Success() {
    StubEmailTemplateService stubService = new StubEmailTemplateService();
    DepositPaidEventListener listener = new DepositPaidEventListener(stubService);

    DepositPaidEvent event =
        new DepositPaidEvent(
            this,
            "res-100",
            "test@example.com",
            "Luxury Penthouse",
            BigDecimal.valueOf(50000000),
            "TXN-123");

    listener.handleDepositPaidEvent(event);

    assertEquals(1, stubService.callCount);
    assertEquals("test@example.com", stubService.sentToEmail);
    assertEquals("res-100", stubService.sentReservationId);
    assertEquals("Luxury Penthouse", stubService.sentPropertyTitle);
    assertEquals(BigDecimal.valueOf(50000000), stubService.sentAmount);
    assertEquals("TXN-123", stubService.sentTransactionId);
  }
}
