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
    PropertyStatus status,
    List<String> imageUrls,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public PropertyResponse(
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
      PropertyStatus status) {
    this(
        id,
        title,
        description,
        propertyType,
        address,
        ward,
        district,
        city,
        area,
        price,
        status,
        List.of(),
        null,
        null);
  }
}
