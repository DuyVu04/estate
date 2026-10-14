package com.project.estate.modules.identity.dto.response;

import com.project.estate.modules.identity.entity.Permission;
import java.util.Set;
import lombok.Builder;

@Builder
public record RoleResponse(String name, String description, Set<Permission> permissions) {}
