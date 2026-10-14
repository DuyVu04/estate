package com.project.estate.modules.identity.dto.response;

import com.project.estate.modules.identity.enums.UserStatus;
import java.util.Set;
import lombok.Builder;

@Builder
public record UserResponse(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    String phone,
    String address,
    UserStatus status,
    boolean enabled,
    Set<RoleResponse> roles) {}
