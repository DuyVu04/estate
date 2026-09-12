package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.response.SystemHealthResponse;
import com.project.estate.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/system-health")
@RequiredArgsConstructor
@Tag(
    name = "Admin System Health",
    description =
        "Endpoints for real-time human-readable system health, JVM metrics, DB pool, and infrastructure diagnostics")
public class SystemHealthController {

  private final SystemHealthService systemHealthService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Lấy toàn bộ thông tin sức khỏe hệ thống (Real-time System Health)",
      description =
          "Đọc metrics từ Micrometer & Actuator, chuyển đổi sang nhãn tiếng Việt dễ hiểu kèm huy hiệu cảnh báo (HEALTHY/WARNING/CRITICAL)")
  public ApiResponse<SystemHealthResponse> getSystemHealth() {
    return ApiResponse.success(systemHealthService.collectHealth());
  }
}
