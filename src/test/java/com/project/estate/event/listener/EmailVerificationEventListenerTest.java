package com.project.estate.event.listener;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.event.EmailVerificationEvent;
import com.project.estate.service.email.EmailTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailVerificationEventListenerTest {

  static class StubEmailTemplateService extends EmailTemplateService {
    String sentEmail;
    String sentToken;
    int callCount = 0;
    boolean shouldThrow = false;

    public StubEmailTemplateService() {
      super(null, null);
    }

    @Override
    public void sendConfirmLink(String email, String token) {
      callCount++;
      if (shouldThrow) {
        throw new RuntimeException("Mail server down");
      }
      this.sentEmail = email;
      this.sentToken = token;
    }
  }

  @Test
  @DisplayName("Should send confirmation link email when EmailVerificationEvent is received")
  void handleEmailVerification_Success() {
    StubEmailTemplateService stubService = new StubEmailTemplateService();
    EmailVerificationEventListener listener = new EmailVerificationEventListener(stubService);
    EmailVerificationEvent event =
        new EmailVerificationEvent("user-123", "test@example.com", "token-abc");

    listener.handleEmailVerification(event);

    assertEquals(1, stubService.callCount);
    assertEquals("test@example.com", stubService.sentEmail);
    assertEquals("token-abc", stubService.sentToken);
  }

  @Test
  @DisplayName("Should catch and log exception without rethrowing when emailTemplateService fails")
  void handleEmailVerification_Failure_DoesNotThrow() {
    StubEmailTemplateService stubService = new StubEmailTemplateService();
    stubService.shouldThrow = true;
    EmailVerificationEventListener listener = new EmailVerificationEventListener(stubService);
    EmailVerificationEvent event =
        new EmailVerificationEvent("user-123", "test@example.com", "token-abc");

    assertDoesNotThrow(() -> listener.handleEmailVerification(event));
    assertEquals(1, stubService.callCount);
  }
}
