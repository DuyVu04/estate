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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reservation")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  /** User Endpoint - Get all reservations for the currently authenticated user. */
  @GetMapping("/my-reservations")
  public ApiResponse<PageResponse<ReservationResponse>> getMyReservations(
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.success(PageResponse.of(reservationService.getMyReservations(pageable)));
  }

  /** User Endpoint Alias - /v1/reservation/me */
  @GetMapping("/me")
  public ApiResponse<PageResponse<ReservationResponse>> getMyReservationsAlias(
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.success(PageResponse.of(reservationService.getMyReservations(pageable)));
  }

  /** Get reservations by explicit userId. */
  @GetMapping("/user/{userId}")
  public ApiResponse<PageResponse<ReservationResponse>> getReservationsByUserId(
      @PathVariable String userId,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.success(
        PageResponse.of(reservationService.getReservationsByUserId(userId, pageable)));
  }

  /** Admin Endpoint - Search and filter all reservations across the system. */
  @GetMapping
  public ApiResponse<PageResponse<ReservationResponse>> getReservations(
      @Filter Specification<Reservation> specification, Pageable pageable) {
    return ApiResponse.success(
        PageResponse.of(reservationService.getReservations(specification, pageable)));
  }

  /** Get single reservation by ID. */
  @GetMapping("/{id}")
  public ApiResponse<ReservationResponse> getReservationById(@PathVariable String id) {
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  /** Create a new reservation for a property (15-min lock). */
  @PostMapping
  public ApiResponse<ReservationResponse> createReservation(
      @Valid @RequestBody ReservationRequest reservation) {
    return ApiResponse.success(reservationService.reserve(reservation));
  }

  /** Pay deposit for a reservation. */
  @PostMapping("/{id}/pay")
  public ApiResponse<ReservationResponse> payDeposit(@PathVariable String id) {
    reservationService.payDeposit(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  /** Admin Endpoint - Mark reservation as completed. */
  @PostMapping("/{id}/complete")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ReservationResponse> completeReservation(@PathVariable String id) {
    reservationService.completeReservation(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  /** Cancel reservation (POST). */
  @PostMapping("/{id}/cancel")
  public ApiResponse<ReservationResponse> cancelReservationPost(@PathVariable String id) {
    reservationService.cancelReservation(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  /** Cancel reservation (DELETE). */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> cancelReservation(@PathVariable String id) {
    reservationService.cancelReservation(id);
    return ApiResponse.success(null);
  }
}
