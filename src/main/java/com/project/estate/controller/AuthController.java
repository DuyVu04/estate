package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.LoginRequest;
import com.project.estate.dto.request.RefreshTokenRequest;
import com.project.estate.dto.request.UserRequest;
import com.project.estate.dto.response.TokenResponse;
import com.project.estate.dto.response.UserResponse;
import com.project.estate.service.AuthService;
import com.project.estate.service.UserService;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UserService userService;

  @Value("${jwt.cookie-name:estate_refresh_token}")
  private String cookieName;

  @Value("${jwt.cookie-secure:false}")
  private boolean cookieSecure;

  @Value("${jwt.cookie-same-site:Lax}")
  private String cookieSameSite;

  @Value("${jwt.refresh-expiration:604800000}")
  private Long refreshTokenExpiration;

  private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/refresh";
  private static final String SESSION_HINT_COOKIE_NAME = "estate_session_hint";
  private static final String SESSION_HINT_PATH = "/";

  @GetMapping("/me")
  public ApiResponse<UserResponse> getMyInfo() {
    return ApiResponse.success(authService.getMyInfo());
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest) {
    TokenResponse tokenResponse = authService.login(loginRequest);

    ResponseCookie refreshCookie =
        ResponseCookie.from(cookieName, tokenResponse.refreshToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(REFRESH_COOKIE_PATH)
            .maxAge(Duration.ofMillis(refreshTokenExpiration))
            .build();

    ResponseCookie sessionHintCookie =
        ResponseCookie.from(SESSION_HINT_COOKIE_NAME, "1")
            .httpOnly(false)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(SESSION_HINT_PATH)
            .maxAge(Duration.ofMillis(refreshTokenExpiration))
            .build();

    // Client nhận accessToken trong JSON Body để lưu vào RAM; refreshToken được bảo vệ trong
    // HttpOnly Cookie
    TokenResponse clientBody =
        TokenResponse.builder().accessToken(tokenResponse.accessToken()).build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .header(HttpHeaders.SET_COOKIE, sessionHintCookie.toString())
        .body(ApiResponse.success(clientBody));
  }

  @PostMapping("/register")
  public ApiResponse<UserResponse> register(@Valid @RequestBody UserRequest userRequest) {
    return ApiResponse.success(userService.createUser(userRequest));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<TokenResponse>> refresh(
      @CookieValue(name = "${jwt.cookie-name:estate_refresh_token}", required = false)
          String cookieRefreshToken,
      @RequestBody(required = false) RefreshTokenRequest requestBody) {

    // Ưu tiên đọc từ HttpOnly Cookie (chuẩn Web), hỗ trợ fallback qua JSON Body (Mobile/Postman)
    String tokenToRefresh =
        (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
            ? cookieRefreshToken
            : (requestBody != null ? requestBody.refreshToken() : null);

    TokenResponse tokenResponse = authService.refreshToken(tokenToRefresh);

    ResponseCookie newRefreshCookie =
        ResponseCookie.from(cookieName, tokenResponse.refreshToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(REFRESH_COOKIE_PATH)
            .maxAge(Duration.ofMillis(refreshTokenExpiration))
            .build();

    ResponseCookie sessionHintCookie =
        ResponseCookie.from(SESSION_HINT_COOKIE_NAME, "1")
            .httpOnly(false)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(SESSION_HINT_PATH)
            .maxAge(Duration.ofMillis(refreshTokenExpiration))
            .build();

    TokenResponse clientBody =
        TokenResponse.builder().accessToken(tokenResponse.accessToken()).build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
        .header(HttpHeaders.SET_COOKIE, sessionHintCookie.toString())
        .body(ApiResponse.success(clientBody));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      authService.logout(authorizationHeader);
    }

    ResponseCookie cleanRefreshCookie =
        ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(REFRESH_COOKIE_PATH)
            .maxAge(0)
            .build();

    ResponseCookie cleanSessionHintCookie =
        ResponseCookie.from(SESSION_HINT_COOKIE_NAME, "")
            .httpOnly(false)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(SESSION_HINT_PATH)
            .maxAge(0)
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString())
        .header(HttpHeaders.SET_COOKIE, cleanSessionHintCookie.toString())
        .body(ApiResponse.success());
  }

  @DeleteMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logoutDelete(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    return logout(authorizationHeader);
  }

  @GetMapping("/verify")
  public ApiResponse<Void> confirmEmail(@RequestParam String token) {
    authService.confirmEmail(token);
    return ApiResponse.success();
  }
}
