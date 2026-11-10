package com.project.estate.modules.payment.event.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.project.estate.modules.identity.service.email.EmailTemplateService;
import com.project.estate.modules.payment.event.DepositPaidEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepositPaidEventListenerTest {

  @Mock private EmailTemplateService emailTemplateService;

  @InjectMocks private DepositPaidEventListener listener;

  @Test
  @DisplayName("Should send deposit paid email when DepositPaidEvent is received")
  void handleDepositPaidEvent_Success() {
    DepositPaidEvent event =
        new DepositPaidEvent(
            this,
            "res-100",
            "test@example.com",
            "Luxury Penthouse",
            BigDecimal.valueOf(50000000),
            "TXN-123");

    listener.handleDepositPaidEvent(event);

    verify(emailTemplateService, times(1))
        .sendDepositPaidEmail(
            eq("test@example.com"),
            eq("res-100"),
            eq("Luxury Penthouse"),
            eq(BigDecimal.valueOf(50000000)),
            eq("TXN-123"));
  }
}
