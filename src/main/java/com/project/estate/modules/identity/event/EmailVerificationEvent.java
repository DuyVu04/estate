package com.project.estate.modules.identity.event;

public record EmailVerificationEvent(String userId, String email, String token) {}
