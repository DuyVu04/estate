package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.common.response.PageResponse;
import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Reservation;
import com.project.estate.service.ReservationService;
import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Reservation & Booking Workflow",
    description =
        "Endpoints for creating reservations with Redisson distributed locking, deposit payment, and workflow lifecycle")
public class ReservationController {

  private final ReservationService reservationService;

  @GetMapping("/my-reservations")
  @Operation(
      summary = "Get my reservations",
      description =
          "Retrieves a paginated list of reservations belonging to the authenticated user")
  public ApiResponse<PageResponse<ReservationResponse>> getMyReservations(
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.success(PageResponse.of(reservationService.getMyReservations(pageable)));
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get my reservations (Alias)",
      description = "Convenience endpoint alias for /my-reservations")
  public ApiResponse<PageResponse<ReservationResponse>> getMyReservationsAlias(
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.success(PageResponse.of(reservationService.getMyReservations(pageable)));
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get reservations by User ID",
      description = "Admin or staff endpoint to view reservations for a specific user")
  public ApiResponse<PageResponse<ReservationResponse>> getReservationsByUserId(
      @PathVariable String userId,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.success(
        PageResponse.of(reservationService.getReservationsByUserId(userId, pageable)));
  }

  @GetMapping
  @Operation(
      summary = "Filter all reservations",
      description =
          "Admin endpoint to query and filter reservations across the entire system with pagination")
  public ApiResponse<PageResponse<ReservationResponse>> getReservations(
      @Filter Specification<Reservation> specification, Pageable pageable) {
    return ApiResponse.success(
        PageResponse.of(reservationService.getReservations(specification, pageable)));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get reservation by ID",
      description = "Retrieves full details of a specific reservation")
  public ApiResponse<ReservationResponse> getReservationById(@PathVariable String id) {
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @PostMapping
  @Operation(
      summary = "Create reservation",
      description =
          "Locks the property using Redisson distributed lock and creates a 15-minute pending reservation")
  public ApiResponse<ReservationResponse> createReservation(
      @Valid @RequestBody ReservationRequest reservation) {
    return ApiResponse.success(reservationService.reserve(reservation));
  }

  @PostMapping("/{id}/pay")
  @Operation(
      summary = "Pay reservation deposit",
      description = "Transition reservation state to DEPOSIT_PAID and advances the workflow engine")
  public ApiResponse<ReservationResponse> payDeposit(@PathVariable String id) {
    reservationService.payDeposit(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Complete reservation",
      description =
          "Admin endpoint to mark the deal completed and transition property status to SOLD")
  public ApiResponse<ReservationResponse> completeReservation(@PathVariable String id) {
    reservationService.completeReservation(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @PostMapping("/{id}/cancel")
  @Operation(
      summary = "Cancel reservation (POST)",
      description =
          "Cancels an active reservation, unlocking the property back to AVAILABLE status")
  public ApiResponse<ReservationResponse> cancelReservationPost(@PathVariable String id) {
    reservationService.cancelReservation(id);
    return ApiResponse.success(reservationService.getReservationById(id));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Cancel reservation (DELETE)",
      description = "Alias RESTful DELETE method to cancel an active reservation")
  public ApiResponse<Void> cancelReservation(@PathVariable String id) {
    reservationService.cancelReservation(id);
    return ApiResponse.success(null);
  }
}
