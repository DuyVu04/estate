package com.project.estate.modules.property.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record RealEstateAdvisorResponse(
    String answer, List<PropertySemanticResponse> recommendedProperties, int totalFound) {}
