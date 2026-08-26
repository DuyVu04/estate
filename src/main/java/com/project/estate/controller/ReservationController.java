package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.common.response.PageResponse;
import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Reservation;
import com.project.estate.service.ReservationService;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reservation")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  @GetMapping
  public ApiResponse<PageResponse<ReservationResponse>> getReservations(
      @Filter Specification<Reservation> specification, Pageable pageable) {
    return ApiResponse.success(
        PageResponse.of(reservationService.getReservations(specification, pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<ReservationResponse> getReservationById(@PathVariable String id) {
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @PostMapping
  public ApiResponse<ReservationResponse> createReservation(
      @Valid @RequestBody ReservationRequest reservation) {
    return ApiResponse.success(reservationService.reserve(reservation));
  }

  @PostMapping("/{id}/pay")
  public ApiResponse<ReservationResponse> payDeposit(@PathVariable String id) {
    reservationService.payDeposit(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ApiResponse<ReservationResponse> completeReservation(@PathVariable String id) {
    reservationService.completeReservation(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<ReservationResponse> cancelReservationPost(@PathVariable String id) {
    reservationService.cancelReservation(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> cancelReservation(@PathVariable String id) {
    reservationService.cancelReservation(id);
    return ApiResponse.success(null);
  }
}
