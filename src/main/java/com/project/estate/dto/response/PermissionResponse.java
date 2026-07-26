package com.project.estate.dto.response;

import lombok.Builder;

@Builder
public record PermissionResponse(
        String name,
        String description
) {
}
