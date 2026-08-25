package com.project.estate.security.service;

import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service quản lý Refresh Token trên Redis In-Memory theo chuẩn RFC 6819. Hỗ trợ Refresh Token
 * Rotation và Reuse Detection (Chống dùng lại token bị đánh cắp).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

  @Value("${jwt.refresh-expiration:86400000}")
  private Long refreshTokenExpiration;

  private final StringRedisTemplate redisTemplate;

  private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
  private static final String USER_REFRESH_PREFIX = "refresh:user:";
  private static final String USED_TOKEN_PREFIX = "refresh:used:";

  // Thời gian lưu vết token đã dùng để phát hiện tấn công tái sử dụng (Reuse Detection)
  private static final Duration USED_TOKEN_GRACE_PERIOD = Duration.ofMinutes(10);

  /**
   * Tạo Refresh Token mới cho người dùng.
   *
   * @param userId ID người dùng
   * @return chuỗi Refresh Token ngẫu nhiên (UUID)
   */
  public String createRefreshToken(String userId) {
    revokeAllRefreshTokensForUser(userId);

    String token = UUID.randomUUID().toString();
    Duration ttl = Duration.ofMillis(refreshTokenExpiration);

    redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + token, userId, ttl);
    redisTemplate.opsForValue().set(USER_REFRESH_PREFIX + userId, token, ttl);

    log.info(
        "[REDIS_REFRESH_TOKEN] Created refresh token for userId={}, ttl={}ms",
        userId,
        refreshTokenExpiration);
    return token;
  }

  /**
   * Xoay vòng Refresh Token (Token Rotation) kèm Phát hiện Tái sử dụng (Reuse Detection).
   *
   * @param oldToken token cũ cần đổi
   * @return token mới vừa được cấp
   */
  public String rotate(String oldToken) {
    if (oldToken == null || oldToken.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 1. Kiểm tra xem Token này có từng bị dùng trước đó không (REUSE ATTACK DETECTION)
    if (Boolean.TRUE.equals(redisTemplate.hasKey(USED_TOKEN_PREFIX + oldToken))) {
      String compromisedUserId = redisTemplate.opsForValue().get(USED_TOKEN_PREFIX + oldToken);
      if (compromisedUserId != null) {
        // Hủy ngay toàn bộ các phiên đăng nhập của người dùng bị nghi lộ token
        revokeAllRefreshTokensForUser(compromisedUserId);
      }
      log.error(
          "[SECURITY_ALERT] Refresh token reuse detected! Token was already consumed: {}. Revoked all sessions for userId={}",
          oldToken,
          compromisedUserId);
      throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
    }

    // 2. Lấy userId từ token đang active
    String userId = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + oldToken);
    if (userId == null) {
      log.warn("[REDIS_REFRESH_TOKEN] Refresh token expired or not found: {}", oldToken);
      throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 3. Đánh dấu token cũ vào danh sách đã sử dụng (Used list) để bắt quả tang nếu có kẻ gian dùng
    // lại
    redisTemplate.opsForValue().set(USED_TOKEN_PREFIX + oldToken, userId, USED_TOKEN_GRACE_PERIOD);
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + oldToken);

    // 4. Cấp token mới
    String newToken = UUID.randomUUID().toString();
    Duration ttl = Duration.ofMillis(refreshTokenExpiration);

    redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + newToken, userId, ttl);
    redisTemplate.opsForValue().set(USER_REFRESH_PREFIX + userId, newToken, ttl);

    log.info(
        "[REDIS_REFRESH_TOKEN] Rotated refresh token for userId={}. Old token marked as used.",
        userId);
    return newToken;
  }

  /**
   * Lấy userId từ Refresh Token trong Redis.
   *
   * @param token chuỗi Refresh Token
   * @return userId tương ứng
   */
  public String getUserIdByToken(String token) {
    String userId = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + token);
    if (userId == null) {
      log.warn("[REDIS_REFRESH_TOKEN] Refresh token expired or not found: {}", token);
      throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
    return userId;
  }

  /**
   * Hủy một Refresh Token cụ thể.
   *
   * @param token token cần hủy
   */
  public void revokeToken(String token) {
    String userId = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + token);
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
    if (userId != null) {
      redisTemplate.delete(USER_REFRESH_PREFIX + userId);
    }
    log.info("[REDIS_REFRESH_TOKEN] Revoked refresh token: {}", token);
  }

  /**
   * Hủy toàn bộ Refresh Token của một người dùng (dùng khi Logout hoặc Đổi mật khẩu).
   *
   * @param userId ID người dùng
   */
  public void revokeAllRefreshTokensForUser(String userId) {
    String token = redisTemplate.opsForValue().get(USER_REFRESH_PREFIX + userId);
    if (token != null) {
      redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
    }
    redisTemplate.delete(USER_REFRESH_PREFIX + userId);
    log.info("[REDIS_REFRESH_TOKEN] Revoked all active refresh tokens for userId={}", userId);
  }
}
