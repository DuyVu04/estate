package com.project.estate.modules.property.dto.request;

import com.project.estate.modules.property.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusUpdateRequest(
    @NotNull(message = "Property status is required") PropertyStatus status) {}
