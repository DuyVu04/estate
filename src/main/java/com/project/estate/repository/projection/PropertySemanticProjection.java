package com.project.estate.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PropertySemanticProjection {
  String getId();

  String getTitle();

  String getDescription();

  String getPropertyType();

  String getAddress();

  String getWard();

  String getDistrict();

  String getCity();

  BigDecimal getArea();

  BigDecimal getPrice();

  String getStatus();

  LocalDateTime getCreatedAt();

  LocalDateTime getUpdatedAt();

  Double getSimilarityScore();
}
