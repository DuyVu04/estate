package com.project.estate.dto.response;

import com.project.estate.enums.UserStatus;
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
