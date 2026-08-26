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
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UserService userService;
  private final AuthCookieFactory cookieFactory;

  @GetMapping("/me")
  public ApiResponse<UserResponse> getMyInfo() {
    return ApiResponse.success(authService.getMyInfo());
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest loginRequest) {
    TokenResponse tokenResponse = authService.login(loginRequest);
    return withCookies(
        cookieFactory.buildAuthCookies(tokenResponse.refreshToken()),
        ApiResponse.success(accessTokenOnly(tokenResponse)));
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
    return withCookies(
        cookieFactory.buildAuthCookies(tokenResponse.refreshToken()),
        ApiResponse.success(accessTokenOnly(tokenResponse)));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      authService.logout(authorizationHeader);
    }
    return withCookies(cookieFactory.buildClearCookies(), ApiResponse.success());
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
