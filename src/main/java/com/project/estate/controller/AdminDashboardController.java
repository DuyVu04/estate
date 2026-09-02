package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.response.AdminDashboardStatsResponse;
import com.project.estate.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(
    name = "Admin Dashboard",
    description = "Endpoints for administrator analytics, KPIs, and overview statistics")
public class AdminDashboardController {

  private final AdminDashboardService adminDashboardService;

  @GetMapping("/stats")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get admin dashboard statistics",
      description =
          "Retrieves aggregate statistics including property counts by status, revenue, total users, and reservation trends")
  public ApiResponse<AdminDashboardStatsResponse> getStats() {
    return ApiResponse.success(adminDashboardService.getStats());
  }
}
