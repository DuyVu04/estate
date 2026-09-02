package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.LoginRequest;
import com.project.estate.dto.request.RefreshTokenRequest;
import com.project.estate.dto.request.UserRequest;
import com.project.estate.dto.response.TokenResponse;
import com.project.estate.dto.response.UserResponse;
import com.project.estate.security.AuthCookieFactory;
import com.project.estate.service.AuthService;
import com.project.estate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    TokenResponse tokenResponse = authService.refreshToken(tokenToRefresh);
    return withCookies(
        cookieFactory.buildAuthCookies(tokenResponse.refreshToken()),
        ApiResponse.success(accessTokenOnly(tokenResponse)));
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

  /** Gắn N dòng Set-Cookie (mỗi cookie 1 dòng riêng) vào response, kèm body bất kỳ. */
  private <T> ResponseEntity<ApiResponse<T>> withCookies(
      List<String> cookies, ApiResponse<T> body) {
    HttpHeaders headers = new HttpHeaders();
    cookies.forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie));
    return ResponseEntity.ok().headers(headers).body(body);
  }
}
