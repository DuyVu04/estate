package com.project.estate.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PropertySemanticResponse(
    String id,
    String title,
    String description,
    String propertyType,
    String address,
    String ward,
    String district,
    String city,
    BigDecimal area,
    BigDecimal price,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Double similarityScore,
    Double matchPercentage) {}
