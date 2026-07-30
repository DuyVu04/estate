package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WorkflowInstanceStatus {
    @JsonProperty("in_progress")
    IN_PROGRESS,

    @JsonProperty("completed")
    COMPLETED,

    @JsonProperty("cancelled")
    CANCELLED,

    @JsonProperty("expired")
    EXPIRED,

    @JsonProperty("failed")
    FAILED
}
