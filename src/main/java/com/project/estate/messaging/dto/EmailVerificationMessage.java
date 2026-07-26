package com.project.estate.messaging.dto;

public record EmailVerificationMessage (
        String userId,

        String email,

        String token,

        int retryCount
){
    public EmailVerificationMessage(
            String userId,
            String email,
            String token
    ) {
        this(userId, email, token, 0);
    }

    public EmailVerificationMessage nextRetry() {
        return new EmailVerificationMessage(
                userId,
                email,
                token,
                retryCount + 1
        );
    }
}
