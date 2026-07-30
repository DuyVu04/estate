package com.project.estate.dto.response;

import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.PropertyType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PropertyResponse(
        String id,
        String title,
        String description,
        PropertyType propertyType,
        String address,
        String ward,
        String district,
        String city,
        BigDecimal area,
        BigDecimal price,
        PropertyStatus status
) {
}
