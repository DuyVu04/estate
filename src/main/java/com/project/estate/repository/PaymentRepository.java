package com.project.estate.repository;

import com.project.estate.entity.Payment;
import com.project.estate.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository
    extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {

  Optional<Payment> findByIdempotencyKey(String idempotencyKey);

  boolean existsByIdempotencyKey(String idempotencyKey);

  List<Payment> findByReservationId(String reservationId);

  @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
  BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
}
