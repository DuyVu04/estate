package com.project.estate.dto.response;

import com.project.estate.enums.WorkflowHistoryStatus;
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
