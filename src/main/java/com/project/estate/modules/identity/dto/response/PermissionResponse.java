package com.project.estate.modules.identity.dto.response;

import lombok.Builder;

@Builder
public record PermissionResponse(String name, String description) {}
