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
 * Service quản lý Refresh Token hoàn toàn trên Redis In-Memory. Tận dụng cơ chế TTL tự động hủy
 * token hết hạn và hỗ trợ Refresh Token Rotation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

  @Value("${jwt.refresh-expiration}")
  private Long refreshTokenExpiration;

  private final StringRedisTemplate redisTemplate;

  private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
  private static final String USER_REFRESH_PREFIX = "refresh:user:";

  /**
   * Tạo Refresh Token mới và lưu vào Redis với TTL.
   *
   * @param userId ID người dùng
   * @return chuỗi Refresh Token ngẫu nhiên (UUID)
   */
  public String createRefreshToken(String userId) {
    revokeAllRefreshTokensForUser(userId);

    String token = UUID.randomUUID().toString();
    Duration ttl = Duration.ofMillis(refreshTokenExpiration);

    // Lưu ánh xạ 2 chiều trong Redis
    redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + token, userId, ttl);
    redisTemplate.opsForValue().set(USER_REFRESH_PREFIX + userId, token, ttl);

    log.info(
        "[REDIS_REFRESH_TOKEN] Created refresh token for userId={}, ttl={}ms",
        userId,
        refreshTokenExpiration);
    return token;
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
   * Xoay vòng Refresh Token (Token Rotation): Hủy token cũ và cấp token mới.
   *
   * @param oldToken token cũ cần xoay vòng
   * @return chuỗi Refresh Token mới
   */
  public String rotate(String oldToken) {
    String userId = getUserIdByToken(oldToken);
    revokeToken(oldToken);
    return createRefreshToken(userId);
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
    log.info("[REDIS_REFRESH_TOKEN] Revoked all refresh tokens for userId={}", userId);
  }
}
