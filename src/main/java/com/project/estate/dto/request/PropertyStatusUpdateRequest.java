package com.project.estate.dto.request;

import com.project.estate.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusUpdateRequest(
    @NotNull(message = "Property status is required") PropertyStatus status) {}
