package com.project.estate.event;

public record EmailVerificationEvent(String userId, String email, String token) {}
