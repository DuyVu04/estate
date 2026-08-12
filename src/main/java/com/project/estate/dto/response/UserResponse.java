package com.project.estate.dto.response;

import com.project.estate.enums.UserStatus;

public record UserResponse(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    UserStatus status) {}
