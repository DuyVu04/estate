package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReservationAction {
    @JsonProperty("create")
    CREATE,

    @JsonProperty("cancel")
    CANCEL,

    @JsonProperty("complete")
    COMPLETE,

    @JsonProperty("expire")
    EXPIRE
}
