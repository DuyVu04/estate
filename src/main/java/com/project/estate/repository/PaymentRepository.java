package com.project.estate.repository;

import com.project.estate.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository
    extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

  Optional<Payment> findByIdempotencyKey(String idempotencyKey);

  boolean existsByIdempotencyKey(String idempotencyKey);

  List<Payment> findByReservationId(String reservationId);
}
