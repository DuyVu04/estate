package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.ResendVerificationRequest;
import com.project.estate.service.AuthService;
import com.project.estate.service.email.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/email")
@Slf4j
@RequiredArgsConstructor
public class EmailController {

  private final MailService emailService;

  private final AuthService authService;

  @GetMapping("/send-email")
  public ApiResponse<Void> sendEmail(
      @RequestParam String to, @RequestParam String subject, @RequestParam String content) {
    emailService.sendText(to, subject, content);
    return ApiResponse.success();
  }

  @PostMapping("/resend-verification-email")
  public ApiResponse<Void> resendVerificationEmail(
      @Valid @RequestBody ResendVerificationRequest request) {

    authService.resendVerificationEmail(request.email());
    return ApiResponse.success();
  }
}
