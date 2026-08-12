package com.project.estate.dto.response;

import com.project.estate.entity.Permission;
import java.util.Set;
import lombok.Builder;

@Builder
public record RoleResponse(String name, String description, Set<Permission> permissions) {}
