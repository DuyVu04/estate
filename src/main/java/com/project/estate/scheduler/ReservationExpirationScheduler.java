package com.project.estate.scheduler;

import com.project.estate.entity.Reservation;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.repository.ReservationRepository;
import com.project.estate.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler scanning for ACTIVE reservations that have passed their expiration threshold (expiresAt < NOW()).
 * Invokes ReservationService.expireReservation() for each expired record,
 * which triggers the WorkflowEngine to execute the EXPIRE strategy,
 * updating Reservation -> EXPIRED, Property -> AVAILABLE, and logging Audit History.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    /**
     * Runs every 60 seconds to clean up expired reservations.
     */
    @Scheduled(fixedRate = 60000)
    public void scanAndExpireReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.ACTIVE, now
        );

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("[EXPIRATION_SCHEDULER] Found {} expired ACTIVE reservation(s) to process.", expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            try {
                reservationService.expireReservation(reservation.getId());
                log.info("[EXPIRATION_SCHEDULER] Successfully expired reservation id={}", reservation.getId());
            } catch (Exception e) {
                log.error("[EXPIRATION_SCHEDULER] Failed to expire reservation id={}: {}",
                        reservation.getId(), e.getMessage(), e);
            }
        }
    }
}
