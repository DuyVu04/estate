package com.project.estate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ReservationRequest(

        @NotNull(message = "Property ID is required")
        String propertyId,

        @NotNull(message = "User ID is required")
        String userId

) {
}
