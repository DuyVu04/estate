package com.project.estate.dto.response;

import com.project.estate.enums.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationResponse(
    String id, PropertyResponse property, ReservationStatus status, LocalDateTime expiresAt) {}
