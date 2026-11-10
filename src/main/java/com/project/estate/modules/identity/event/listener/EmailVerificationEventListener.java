package com.project.estate.modules.identity.event.listener;

import com.project.estate.modules.identity.event.EmailVerificationEvent;
import com.project.estate.modules.identity.service.email.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationEventListener {

  private final EmailTemplateService emailTemplateService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleEmailVerification(EmailVerificationEvent event) {
    log.info(
        "[EVENT_LISTENER] Received EmailVerificationEvent for userId={}, email={}",
        event.userId(),
        event.email());
    try {
      emailTemplateService.sendConfirmLink(event.email(), event.token());
      log.info("[EVENT_LISTENER] Verification email sent successfully to {}", event.email());
    } catch (Exception e) {
      log.error("[EVENT_LISTENER] Failed to send verification email to {}", event.email(), e);
    }
  }
}
