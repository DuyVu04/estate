package com.project.estate.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "Success", HttpStatus.OK),

    INVALID_REQUEST(1000, "Invalid request", HttpStatus.BAD_REQUEST),

    VALIDATION_ERROR(1001, "Validation failed", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND(1100, "User not found", HttpStatus.NOT_FOUND),

    EMAIL_ALREADY_EXISTS(1101, "Email already exists", HttpStatus.CONFLICT),

    PHONE_ALREADY_EXISTS(1102, "Phone already exists", HttpStatus.CONFLICT),

    INVALID_PASSWORD(1103, "Invalid password", HttpStatus.BAD_REQUEST),

    EMAIL_NOT_VERIFIED(1104, "Email has not been verified", HttpStatus.BAD_REQUEST),

    EMAIL_ALREADY_VERIFIED(1105, "Email already verified", HttpStatus.BAD_REQUEST),

    INVALID_USERNAME_OR_PASSWORD(1113,"Username or password invalid",HttpStatus.BAD_REQUEST),

    INVALID_VERIFICATION_TOKEN(
            1106,
            "Verification token is invalid or expired",
            HttpStatus.BAD_REQUEST
    ),

    TOKEN_EXPIRED(
            1107,
            "Token expired",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_REFRESH_TOKEN(
            1108,
            "Invalid refresh token",
            HttpStatus.BAD_REQUEST
    ),

    REFRESH_TOKEN_REVOKED(
            1109,
            "Refresh token is revoked",
            HttpStatus.BAD_REQUEST
    ),

    REFRESH_TOKEN_EXPIRED(
            1110,
            "Refresh token was expired. Please make a new sign in request",
            HttpStatus.BAD_REQUEST
    ),

    EMAIL_SENDING_FAILED(
            1111,
            "Failed to send email",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    ROLE_NOT_FOUND(
            1112,
            "Role not found",
            HttpStatus.NOT_FOUND
    ),

    PROPERTY_NOT_FOUND(
            2100,
            "Property not found",
            HttpStatus.NOT_FOUND
    ),

    UNAUTHENTICATED(
            1200,
            "Unauthenticated",
            HttpStatus.UNAUTHORIZED
    ),

    ACCESS_DENIED(
            1201,
            "Access denied",
            HttpStatus.FORBIDDEN
    ),

    RESOURCE_NOT_FOUND(
            1300,
            "Resource not found",
            HttpStatus.NOT_FOUND
    ),



    TOO_MANY_REQUESTS(
            1301,
            "Too many login attempts. Please try again later.",
            HttpStatus.TOO_MANY_REQUESTS
    ),
    PROPERTY_NOT_AVAILABLE(
            1302,
            "Property not available",
            HttpStatus.NOT_FOUND
    ),

    PROPERTY_ALREADY_RESERVED(
            1303,
            "Property is already reserved",
            HttpStatus.NOT_FOUND
    ),

    INVALID_STATE_TRANSITION(
            1400,
            "Invalid workflow state transition",
            HttpStatus.BAD_REQUEST
    ),

    UNAUTHORIZED_WORKFLOW_ACTOR(
            1401,
            "Actor is not authorized for this workflow transition",
            HttpStatus.FORBIDDEN
    ),

    INTERNAL_SERVER_ERROR(
            9999,
            "Internal server error",
            HttpStatus.INTERNAL_SERVER_ERROR
    );


    private final int code;
    private final String message;
    private final HttpStatus status;
}
