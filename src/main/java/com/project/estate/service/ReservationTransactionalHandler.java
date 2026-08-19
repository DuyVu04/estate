package com.project.estate.service;

import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Property;
import com.project.estate.entity.Reservation;
import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.ReservationMapper;
import com.project.estate.repository.PropertyRepository;
import com.project.estate.repository.ReservationRepository;
import com.project.estate.repository.UserRepository;
import com.project.estate.workflow.annotation.WorkflowEngine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationTransactionalHandler {

  private final ReservationRepository reservationRepository;
  private final PropertyRepository propertyRepository;
  private final UserRepository userRepository;
  private final ReservationMapper reservationMapper;

  @WorkflowEngine(
      action = ReservationAction.CREATE,
      step = "create-reservation",
      targetIdSpel = "#result.id")
  @Transactional
  public ReservationResponse executeReserve(ReservationRequest reservationRequest) {
    Property property =
        propertyRepository
            .findById(reservationRequest.propertyId())
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    if (property.getStatus() != PropertyStatus.AVAILABLE) {
      throw new AppException(ErrorCode.PROPERTY_NOT_AVAILABLE);
    }

    boolean exists =
        reservationRepository.existsByPropertyIdAndStatus(
            reservationRequest.propertyId(), ReservationStatus.ACTIVE);

    if (exists) {
      throw new AppException(ErrorCode.PROPERTY_ALREADY_RESERVED);
    }

    var user =
        userRepository
            .findById(reservationRequest.userId())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    Reservation reservation =
        Reservation.builder()
            .user(user)
            .property(property)
            .status(ReservationStatus.ACTIVE)
            .depositAmount(property.getPrice().multiply(BigDecimal.valueOf(0.1)))
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build();

    reservationRepository.save(reservation);
    property.setStatus(PropertyStatus.RESERVED);

    return reservationMapper.toResponse(reservation);
  }

  @WorkflowEngine(
      action = ReservationAction.CANCEL,
      step = "cancel-reservation",
      targetIdSpel = "#id")
  @Transactional
  public void executeCancel(String id) {
    Reservation reservation =
        reservationRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.getProperty().setStatus(PropertyStatus.AVAILABLE);
  }

  @WorkflowEngine(
      action = ReservationAction.COMPLETE,
      step = "complete-reservation",
      targetIdSpel = "#id")
  @Transactional
  public void executeComplete(String id) {
    Reservation reservation =
        reservationRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    reservation.setStatus(ReservationStatus.COMPLETED);
    reservation.getProperty().setStatus(PropertyStatus.SOLD);
  }

  @WorkflowEngine(
      action = ReservationAction.EXPIRE,
      step = "expire-reservation",
      targetIdSpel = "#id")
  @Transactional
  public void executeExpire(String id) {
    Reservation reservation =
        reservationRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    reservation.setStatus(ReservationStatus.EXPIRED);
    reservation.getProperty().setStatus(PropertyStatus.AVAILABLE);
  }

  @WorkflowEngine(
      action = ReservationAction.PAY_DEPOSIT,
      step = "pay-deposit",
      targetIdSpel = "#id")
  @Transactional
  public void executePayDeposit(String id) {
    Reservation reservation =
        reservationRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    reservation.setStatus(ReservationStatus.DEPOSIT_PAID);
  }
}
