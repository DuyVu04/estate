package com.project.estate.modules.reservation.dto.response;

import com.project.estate.modules.reservation.workflow.enums.WorkflowHistoryStatus;
import java.time.LocalDateTime;

public record WorkflowHistoryResponse(
    String id,
    String action,
    String step,
    String previousStatus,
    String newStatus,
    String performedBy,
    WorkflowHistoryStatus status,
    String errorMessage,
    LocalDateTime timestamp) {}
