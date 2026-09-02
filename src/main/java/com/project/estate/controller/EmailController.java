package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.ResendVerificationRequest;
import com.project.estate.service.AuthService;
import com.project.estate.service.email.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/email")
@Slf4j
@RequiredArgsConstructor
@Tag(
    name = "Email Service",
    description = "Endpoints for sending notification and account verification emails")
public class EmailController {

  private final MailService emailService;
  private final AuthService authService;

  @GetMapping("/send-email")
  @Operation(
      summary = "Send plain text email",
      description = "Sends a generic plain text email notification to a specified recipient")
  public ApiResponse<Void> sendEmail(
      @RequestParam String to, @RequestParam String subject, @RequestParam String content) {
    emailService.sendText(to, subject, content);
    return ApiResponse.success();
  }

  @PostMapping("/resend-verification-email")
  @Operation(
      summary = "Resend verification email",
      description =
          "Generates and sends a new verification link for unverified user email accounts")
  public ApiResponse<Void> resendVerificationEmail(
      @Valid @RequestBody ResendVerificationRequest request) {

    authService.resendVerificationEmail(request.email());
    return ApiResponse.success();
  }
}
