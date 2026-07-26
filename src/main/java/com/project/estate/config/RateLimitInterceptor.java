package com.project.estate.config;

import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    private static final Map<String, RateLimitRule> RULES = Map.of(
            "/v1/auth/login", new RateLimitRule(5, Duration.ofMinutes(1)),
            "/v1/auth/resend-verification-email", new RateLimitRule(3, Duration.ofMinutes(10))
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateLimitRule rule = RULES.get(request.getServletPath());
        if (rule == null) {
            return true; // endpoint không nằm trong danh sách cần rate-limit -> cho qua luôn
        }

        String ip = resolveIp(request);
        String key = "rate_limit:" + request.getServletPath() + ":" + ip;

        if (!rateLimitService.isAllowed(key, rule.limit(), rule.window())) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return true;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private record RateLimitRule(int limit, Duration window) {}
}
