package com.project.estate.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record RealEstateAdvisorResponse(
    String answer, List<PropertySemanticResponse> recommendedProperties, int totalFound) {}
