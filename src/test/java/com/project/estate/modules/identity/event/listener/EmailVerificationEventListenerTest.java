package com.project.estate.modules.identity.event.listener;

import static org.mockito.Mockito.*;

import com.project.estate.modules.identity.event.EmailVerificationEvent;
import com.project.estate.modules.identity.service.email.EmailTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationEventListenerTest {

  @Mock private EmailTemplateService emailTemplateService;

  @InjectMocks private EmailVerificationEventListener listener;

  @Test
  @DisplayName("Should send confirmation link email when EmailVerificationEvent is received")
  void handleEmailVerification_Success() {
    EmailVerificationEvent event =
        new EmailVerificationEvent("user-123", "test@example.com", "token-abc");

    listener.handleEmailVerification(event);

    verify(emailTemplateService, times(1)).sendConfirmLink("test@example.com", "token-abc");
  }

  @Test
  @DisplayName("Should catch and log exception without rethrowing when emailTemplateService fails")
  void handleEmailVerification_Failure_DoesNotThrow() {
    EmailVerificationEvent event =
        new EmailVerificationEvent("user-123", "test@example.com", "token-abc");

    doThrow(new RuntimeException("Mail server down"))
        .when(emailTemplateService)
        .sendConfirmLink(any(), any());

    listener.handleEmailVerification(event);

    verify(emailTemplateService, times(1)).sendConfirmLink("test@example.com", "token-abc");
  }
}
