package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WorkflowHistoryStatus {
  @JsonProperty("success")
  SUCCESS,

  @JsonProperty("failed")
  FAILED
}
