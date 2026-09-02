package com.project.estate.dto.request;

import com.project.estate.enums.PropertyType;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record PropertyUpdateRequest(
    String title,
    String description,
    PropertyType propertyType,
    String address,
    String ward,
    String district,
    String city,
    @Positive(message = "Area must be greater than 0") BigDecimal area,
    @Positive(message = "Price must be greater than 0") BigDecimal price,
    Integer bedrooms,
    Integer bathrooms,
    String thumbnailUrl,
    List<String> imageUrls) {}
