package com.project.estate.scheduler;

import com.project.estate.entity.Reservation;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.repository.ReservationRepository;
import com.project.estate.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationExpirationScheduler scheduler;

    @Test
    @DisplayName("Should process expired reservations when found")
    void scanAndExpireReservations_WhenExpiredFound_InvokesExpireReservation() {
        Reservation res1 = Reservation.builder().id("res-001").status(ReservationStatus.ACTIVE).expiresAt(LocalDateTime.now().minusMinutes(5)).build();
        Reservation res2 = Reservation.builder().id("res-002").status(ReservationStatus.ACTIVE).expiresAt(LocalDateTime.now().minusMinutes(1)).build();

        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(res1, res2));

        scheduler.scanAndExpireReservations();

        verify(reservationService, times(1)).expireReservation("res-001");
        verify(reservationService, times(1)).expireReservation("res-002");
    }

    @Test
    @DisplayName("Should do nothing when no expired reservations exist")
    void scanAndExpireReservations_WhenNoneFound_DoesNothing() {
        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.scanAndExpireReservations();

        verify(reservationService, never()).expireReservation(any());
    }
}
