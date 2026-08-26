package com.project.estate.security;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Tập trung toàn bộ logic tạo/xóa cookie liên quan đến Auth (Refresh Token + Session Hint).
 * Controller chỉ gọi ra, không tự build ResponseCookie nữa — sửa cấu hình chỉ cần sửa 1 nơi.
 */
@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

  @Value("${jwt.cookie-name:estate_refresh_token}")
  private String refreshCookieName;

  @Value("${jwt.cookie-secure:false}")
  private boolean cookieSecure;

  @Value("${jwt.cookie-same-site:Lax}")
  private String cookieSameSite;

  @Value("${jwt.refresh-expiration:604800000}")
  private long refreshTokenExpirationMs;

  private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/refresh";
  private static final String SESSION_HINT_COOKIE_NAME = "estate_session_hint";
  private static final String SESSION_HINT_PATH = "/";

  /** Tạo cặp cookie (refreshToken + sessionHint) khi login/refresh thành công. */
  public List<String> buildAuthCookies(String refreshTokenValue) {
    return List.of(
        buildCookie(
            refreshCookieName,
            refreshTokenValue,
            REFRESH_COOKIE_PATH,
            true,
            Duration.ofMillis(refreshTokenExpirationMs)),
        buildCookie(
            SESSION_HINT_COOKIE_NAME,
            "1",
            SESSION_HINT_PATH,
            false,
            Duration.ofMillis(refreshTokenExpirationMs)));
  }

  /** Tạo cặp cookie rỗng (Max-Age=0) để xóa khi logout. */
  public List<String> buildClearCookies() {
    return List.of(
        buildCookie(refreshCookieName, "", REFRESH_COOKIE_PATH, true, Duration.ZERO),
        buildCookie(SESSION_HINT_COOKIE_NAME, "", SESSION_HINT_PATH, false, Duration.ZERO));
  }

  private String buildCookie(
      String name, String value, String path, boolean httpOnly, Duration maxAge) {
    return ResponseCookie.from(name, value)
        .httpOnly(httpOnly)
        .secure(cookieSecure)
        .sameSite(cookieSameSite)
        .path(path)
        .maxAge(maxAge)
        .build()
        .toString();
  }
}
