package com.project.estate.dto.response;

import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.PropertyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
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
    Integer bedrooms,
    Integer bathrooms,
    PropertyStatus status,
    String thumbnailUrl,
    List<String> imageUrls,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
