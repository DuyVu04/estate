package com.project.estate.dto.response;

import com.project.estate.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record ReservationResponse(
    String id,
    PropertyResponse property,
    UserResponse customer,
    ReservationStatus status,
    BigDecimal depositAmount,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    List<WorkflowHistoryResponse> histories) {}
