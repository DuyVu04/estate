package com.project.estate.modules.system.service;

import com.project.estate.modules.payment.enums.PaymentStatus;
import com.project.estate.modules.payment.repository.PaymentRepository;
import com.project.estate.modules.property.enums.PropertyStatus;
import com.project.estate.modules.property.repository.PropertyRepository;
import com.project.estate.modules.reservation.enums.ReservationStatus;
import com.project.estate.modules.reservation.repository.ReservationRepository;
import com.project.estate.modules.system.dto.response.AdminDashboardStatsResponse;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

  private final PropertyRepository propertyRepository;
  private final ReservationRepository reservationRepository;
  private final PaymentRepository paymentRepository;

  @Transactional(readOnly = true)
  public AdminDashboardStatsResponse getStats() {
    long totalProperties = propertyRepository.count();
    long availableProperties = propertyRepository.countByStatus(PropertyStatus.AVAILABLE);
    long activeReservations = reservationRepository.countByStatus(ReservationStatus.ACTIVE);
    long depositPaidReservations =
        reservationRepository.countByStatus(ReservationStatus.DEPOSIT_PAID);
    long completedReservations = reservationRepository.countByStatus(ReservationStatus.COMPLETED);
    BigDecimal totalDepositRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);

    return AdminDashboardStatsResponse.builder()
        .totalProperties(totalProperties)
        .availableProperties(availableProperties)
        .activeReservations(activeReservations)
        .depositPaidReservations(depositPaidReservations)
        .completedReservations(completedReservations)
        .totalDepositRevenue(totalDepositRevenue)
        .build();
  }
}
