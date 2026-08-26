package com.project.estate.repository;

import com.project.estate.entity.Reservation;
import com.project.estate.enums.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository
    extends JpaRepository<Reservation, String>, JpaSpecificationExecutor<Reservation> {

  boolean existsByPropertyIdAndStatus(String propertyId, ReservationStatus status);

  List<Reservation> findByStatusAndExpiresAtBefore(
      ReservationStatus status, LocalDateTime dateTime);

  Page<Reservation> findByUserId(String userId, Pageable pageable);
}
