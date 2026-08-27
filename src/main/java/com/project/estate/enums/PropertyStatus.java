package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PropertyStatus {
  @JsonProperty("available")
  AVAILABLE,

  @JsonProperty("reserved")
  RESERVED,

  @JsonProperty("sold")
  SOLD,

  @JsonProperty("pending_approval")
  PENDING_APPROVAL,

  @JsonProperty("rejected")
  REJECTED,

  @JsonProperty("hidden")
  HIDDEN
}
