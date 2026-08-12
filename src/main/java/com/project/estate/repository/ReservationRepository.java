package com.project.estate.repository;

import com.project.estate.entity.Reservation;
import com.project.estate.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository
    extends JpaRepository<Reservation, String>, JpaSpecificationExecutor<Reservation> {

  boolean existsByPropertyIdAndStatus(String propertyId, ReservationStatus status);

  java.util.List<Reservation> findByStatusAndExpiresAtBefore(
      ReservationStatus status, java.time.LocalDateTime dateTime);
}
