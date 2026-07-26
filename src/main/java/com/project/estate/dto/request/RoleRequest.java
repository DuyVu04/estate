package com.project.estate.dto.request;

import java.util.Set;


public record RoleRequest(
        String name,
        String description,
        Set<String> permissions
) {
}
