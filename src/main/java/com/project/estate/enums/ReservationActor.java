package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReservationActor {
  @JsonProperty("customer")
  CUSTOMER,

  @JsonProperty("admin")
  ADMIN,

  @JsonProperty("system")
  SYSTEM
}
