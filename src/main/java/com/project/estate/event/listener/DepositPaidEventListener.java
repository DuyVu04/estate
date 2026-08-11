package com.project.estate.event.listener;

import com.project.estate.event.DepositPaidEvent;
import com.project.estate.service.email.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositPaidEventListener {

    private final EmailTemplateService emailTemplateService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDepositPaidEvent(DepositPaidEvent event) {
        log.info("[EVENT_LISTENER] Received DepositPaidEvent for reservationId={}, userEmail={}",
                event.getReservationId(), event.getUserEmail());
        try {
            emailTemplateService.sendDepositPaidEmail(
                    event.getUserEmail(),
                    event.getReservationId(),
                    event.getPropertyTitle(),
                    event.getAmount(),
                    event.getTransactionRef()
            );
            log.info("[EVENT_LISTENER] Confirmation email sent successfully to {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("[EVENT_LISTENER] Failed to send deposit confirmation email to {}", event.getUserEmail(), e);
        }
    }
}
