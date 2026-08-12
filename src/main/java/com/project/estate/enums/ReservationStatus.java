package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReservationStatus {
  @JsonProperty("active")
  ACTIVE,

  @JsonProperty("expired")
  EXPIRED,

  @JsonProperty("cancelled")
  CANCELLED,

  @JsonProperty("completed")
  COMPLETED,

  @JsonProperty("deposit_paid")
  DEPOSIT_PAID
}
