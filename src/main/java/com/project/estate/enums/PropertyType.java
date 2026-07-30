package com.project.estate.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Property type enumeration
 * Represents different types of properties available in the system
 */
public enum PropertyType {
    @JsonProperty("apartment")
    APARTMENT,
    
    @JsonProperty("house")
    HOUSE,
    
    @JsonProperty("villa")
    VILLA,
    
    @JsonProperty("land")
    LAND,
    
    @JsonProperty("shophouse")
    SHOPHOUSE
}
