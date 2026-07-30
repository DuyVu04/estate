package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Property status enumeration
 * Represents the availability status of a property
 */
public enum PropertyStatus {
    @JsonProperty("available")
    AVAILABLE,
    
    @JsonProperty("reserved")
    RESERVED,
    
    @JsonProperty("sold")
    SOLD
}
