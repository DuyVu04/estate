package com.project.estate.service.redis;

import com.project.estate.security.jwt.JwtService;
import java.time.Duration;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

  private final StringRedisTemplate redisTemplate;
  private final JwtService jwtService;

  private static final String BLACKLIST_PREFIX = "blacklist:jwt:";

  /**
   * Đưa JWT Access Token vào Redis Blacklist với TTL bằng thời gian sống còn lại của token.
   *
   * @param token JWT token cần đưa vào danh sách đen
   */
  public void blacklistToken(String token) {
    try {
      Date expiration = jwtService.extractExpiration(token);
      long remainingTimeMs = expiration.getTime() - System.currentTimeMillis();

      if (remainingTimeMs > 0) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(remainingTimeMs));
        log.info("[REDIS_BLACKLIST] Token blacklisted successfully for {} ms", remainingTimeMs);
      }
    } catch (Exception e) {
      log.warn("[REDIS_BLACKLIST] Failed to blacklist token: {}", e.getMessage());
    }
  }

  /**
   * Kiểm tra xem JWT token có nằm trong Blacklist hay không.
   *
   * @param token JWT token cần kiểm tra
   * @return true nếu token đã bị hủy/đăng xuất
   */
  public boolean isBlacklisted(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
  }
}
