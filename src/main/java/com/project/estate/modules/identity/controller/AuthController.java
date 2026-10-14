package com.project.estate.modules.identity.controller;

import com.project.estate.modules.identity.dto.request.LoginRequest;
import com.project.estate.modules.identity.dto.request.RefreshTokenRequest;
import com.project.estate.modules.identity.dto.request.UserRequest;
import com.project.estate.modules.identity.dto.response.TokenResponse;
import com.project.estate.modules.identity.dto.response.UserResponse;
import com.project.estate.modules.identity.security.AuthCookieFactory;
import com.project.estate.modules.identity.service.AuthService;
import com.project.estate.modules.identity.service.UserService;
import com.project.estate.shared.exception.AppException;
import com.project.estate.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication & Security",
    description =
        "Endpoints for user registration, login, token refresh, logout and email verification")
public class AuthController {

  private final AuthService authService;
  private final UserService userService;
  private final AuthCookieFactory cookieFactory;

  @GetMapping("/me")
  @Operation(
      summary = "Get current user profile",
      description = "Retrieves information and roles for the currently authenticated user")
  public ApiResponse<UserResponse> getMyInfo() {
    return ApiResponse.success(authService.getMyInfo());
  }

  @PostMapping("/login")
  @Operation(
      summary = "User Login",
      description =
          "Authenticates user credentials and returns JWT Access Token along with setting HttpOnly Refresh Cookie")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest) {
    TokenResponse tokenResponse = authService.login(loginRequest);
    return withCookies(
        cookieFactory.buildAuthCookies(tokenResponse.refreshToken()),
        ApiResponse.success(accessTokenOnly(tokenResponse)));
  }

  @PostMapping("/register")
  @Operation(
      summary = "Register new account",
      description = "Creates a new customer user account and sends verification email")
  public ApiResponse<UserResponse> register(@Valid @RequestBody UserRequest userRequest) {
    return ApiResponse.success(userService.createUser(userRequest));
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh JWT access token",
      description =
          "Generates a new access token using the refresh token from HttpOnly cookie or request body")
  public ResponseEntity<ApiResponse<TokenResponse>> refresh(
      @CookieValue(name = "${jwt.cookie-name:estate_refresh_token}", required = false)
          String cookieRefreshToken,
      @RequestBody(required = false) RefreshTokenRequest requestBody) {

    // Ưu tiên đọc từ HttpOnly Cookie (chuẩn Web), hỗ trợ fallback qua JSON Body (Mobile/Postman)
    String tokenToRefresh =
        (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
            ? cookieRefreshToken
            : (requestBody != null ? requestBody.refreshToken() : null);

    try {
      TokenResponse tokenResponse = authService.refreshToken(tokenToRefresh);
      return withCookies(
          cookieFactory.buildAuthCookies(tokenResponse.refreshToken()),
          ApiResponse.success(accessTokenOnly(tokenResponse)));
    } catch (AppException ex) {
      log.warn(
          "[AUTH_REFRESH_FAILED] Refresh token failed: code={}, message={}. Clearing auth cookies to prevent reload loop.",
          ex.getErrorCode().getCode(),
          ex.getErrorCode().getMessage());
      return withCookies(
          cookieFactory.buildClearCookies(),
          ApiResponse.error(ex.getErrorCode()),
          ex.getErrorCode().getStatus());
    }
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Logout user (POST)",
      description = "Blacklists the current access token and clears HttpOnly refresh cookie")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      authService.logout(authorizationHeader);
    }
    return withCookies(cookieFactory.buildClearCookies(), ApiResponse.success());
  }

  @DeleteMapping("/logout")
  @Operation(
      summary = "Logout user (DELETE)",
      description = "Alias endpoint for logout with DELETE method")
  public ResponseEntity<ApiResponse<Void>> logoutDelete(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    return logout(authorizationHeader);
  }

  @GetMapping("/verify")
  @Operation(
      summary = "Verify account email",
      description = "Confirms user email address using the verification token sent via email")
  public ApiResponse<Void> confirmEmail(@RequestParam String token) {
    authService.confirmEmail(token);
    return ApiResponse.success();
  }

  // ---- helpers ----

  /** Client chỉ nhận accessToken trong Body; refreshToken luôn nằm trong HttpOnly Cookie. */
  private TokenResponse accessTokenOnly(TokenResponse tokenResponse) {
    return TokenResponse.builder().accessToken(tokenResponse.accessToken()).build();
  }

  /**
   * Gắn N dòng Set-Cookie (mỗi cookie 1 dòng riêng) vào response, kèm body bất kỳ (mặc định HTTP
   * 200).
   */
  private <T> ResponseEntity<ApiResponse<T>> withCookies(
      List<String> cookies, ApiResponse<T> body) {
    return withCookies(cookies, body, HttpStatus.OK);
  }

  /**
   * Gắn N dòng Set-Cookie vào response kèm custom HttpStatus (dùng khi refresh thất bại trả 401).
   */
  private <T> ResponseEntity<ApiResponse<T>> withCookies(
      List<String> cookies, ApiResponse<T> body, HttpStatus status) {
    HttpHeaders headers = new HttpHeaders();
    cookies.forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie));
    return ResponseEntity.status(status).headers(headers).body(body);
  }
}
