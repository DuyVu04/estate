package com.project.estate.dto.response;

import com.project.estate.entity.Permission;
import lombok.Builder;

import java.util.Set;

@Builder
public record RoleResponse(
        String name,
        String description,
        Set<Permission> permissions
) {
}
