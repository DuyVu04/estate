package com.project.estate.modules.identity.service;

import com.project.estate.modules.identity.dto.request.LoginRequest;
import com.project.estate.modules.identity.dto.response.TokenResponse;
import com.project.estate.modules.identity.dto.response.UserResponse;
import com.project.estate.modules.identity.entity.User;
import com.project.estate.modules.identity.enums.UserStatus;
import com.project.estate.modules.identity.mapper.UserMapper;
import com.project.estate.modules.identity.messaging.dto.EmailVerificationMessage;
import com.project.estate.modules.identity.messaging.producer.EmailProducer;
import com.project.estate.modules.identity.repository.UserRepository;
import com.project.estate.modules.identity.security.UserPrincipal;
import com.project.estate.modules.identity.security.jwt.JwtService;
import com.project.estate.modules.identity.security.service.RefreshTokenService;
import com.project.estate.modules.identity.service.redis.TokenBlacklistService;
import com.project.estate.modules.identity.service.redis.VerificationTokenService;
import com.project.estate.shared.enums.ErrorCode;
import com.project.estate.shared.exception.AppException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final AuthenticationManager authenticationManager;

  private final JwtService jwtService;

  private final RefreshTokenService refreshTokenService;

  private final TokenBlacklistService tokenBlacklistService;

  private final UserRepository userRepository;

  private final UserMapper userMapper;

  private final VerificationTokenService verificationTokenService;

  private final EmailProducer emailProducer;

  public UserResponse getMyInfo() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    User user =
        userRepository
            .findById(principal.getUser().getId())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    return userMapper.toUserResponse(user);
  }

  public TokenResponse login(LoginRequest loginRequest) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.username(), loginRequest.password()));
      UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

      String accessToken = jwtService.generateToken(user);

      String refreshToken = refreshTokenService.createRefreshToken(user.getUser().getId());

      return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    } catch (AuthenticationException ex) {
      log.warn("Login failed for user {}: {}", loginRequest.username(), ex.getMessage());
      throw new AppException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
    }
  }

  public TokenResponse refreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
    String newRefreshToken = refreshTokenService.rotate(refreshToken);
    String userId = refreshTokenService.getUserIdByToken(newRefreshToken);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    String newAccessToken = jwtService.generateToken(new UserPrincipal(user));

    return TokenResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .build();
  }

  public void logout(String authorizationHeader) {
    String token = authorizationHeader.substring(7);
    tokenBlacklistService.blacklistToken(token);
    String userName = jwtService.extractUsername(token);
    var user =
        userRepository
            .findByUsername(userName)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    refreshTokenService.revokeAllRefreshTokensForUser(user.getId());
  }

  @Transactional
  public void confirmEmail(String token) {
    String email = verificationTokenService.getEmailByToken(token);

    if (email == null) {
      throw new AppException(ErrorCode.TOKEN_EXPIRED);
    }

    var user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    if (user.isEnabled()) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
    }

    user.setStatus(UserStatus.ACTIVE);
    user.setEnabled(true);

    userRepository.save(user);

    verificationTokenService.deleteByToken(token);
    verificationTokenService.deleteByEmail(email);
  }

  @Transactional
  public void resendVerificationEmail(String email) {
    var user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    if (user.isEnabled()) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
    }
    String oldToken = verificationTokenService.getTokenByEmail(email);
    String newToken = UUID.randomUUID().toString();
    verificationTokenService.save(newToken, email);
    if (oldToken != null) {
      verificationTokenService.deleteByToken(oldToken);
    }
    emailProducer.send(new EmailVerificationMessage(user.getId(), user.getEmail(), newToken));
  }
}
