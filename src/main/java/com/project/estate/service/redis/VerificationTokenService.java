package com.project.estate.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration EXPIRE_TIME = Duration.ofMinutes(5);

    private static final String TOKEN_PREFIX = "verify:token:";

    private static final String EMAIL_PREFIX = "verify:email:";

    public void save(String token, String email) {
        log.info("Saving token {} for {}", token, email);
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                email,
                EXPIRE_TIME
        );
        redisTemplate.opsForValue().set(
                EMAIL_PREFIX + email,
                token,
                EXPIRE_TIME
        );
        log.info("Saved");
    }

    public String getEmailByToken(String token) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
    }

    public void deleteByToken(String token) {
        redisTemplate.delete(TOKEN_PREFIX + token);
    }

    public String getTokenByEmail(String email) {
        return redisTemplate.opsForValue().get(EMAIL_PREFIX + email);
    }

    public void deleteByEmail(String email) {
        redisTemplate.delete(EMAIL_PREFIX + email);
    }

}
