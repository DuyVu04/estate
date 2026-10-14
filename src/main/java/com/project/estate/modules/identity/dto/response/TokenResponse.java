package com.project.estate.modules.identity.dto.response;

import lombok.Builder;

@Builder
public record TokenResponse(String accessToken, String refreshToken) {}
