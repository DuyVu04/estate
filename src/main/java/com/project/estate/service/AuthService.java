package com.project.estate.service;

import com.project.estate.dto.request.LoginRequest;
import com.project.estate.dto.request.RefreshTokenRequest;
import com.project.estate.dto.response.TokenResponse;
import com.project.estate.entity.RefreshToken;
import com.project.estate.entity.User;
import com.project.estate.messaging.dto.EmailVerificationMessage;
import com.project.estate.messaging.producer.EmailProducer;
import com.project.estate.repository.UserRepository;
import com.project.estate.exception.AppException;
import com.project.estate.enums.ErrorCode;
import com.project.estate.security.UserPrincipal;
import com.project.estate.security.jwt.JwtService;
import com.project.estate.security.service.RefreshTokenService;
import com.project.estate.service.redis.VerificationTokenService;
import com.project.estate.enums.UserStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;

import java.util.UUID;

import com.project.estate.dto.response.UserResponse;
import com.project.estate.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final VerificationTokenService verificationTokenService;

    private final EmailProducer emailProducer;

    public UserResponse getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );
            UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

            String accessToken = jwtService.generateToken(user);

            RefreshToken refreshToken =
                    refreshTokenService.createRefreshToken(user.getUser().getId());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .build();
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("Login failed for user {}: {}", loginRequest.username(), ex.getMessage());
            throw new AppException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshTokenEntity = refreshTokenService.findByToken(request.refreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (refreshTokenEntity.isRevoked()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }
        refreshTokenService.verifyExpiration(refreshTokenEntity);
        RefreshToken newRefresh = refreshTokenService.rotate(refreshTokenEntity);

        User user = newRefresh.getUser();

        String newAccessToken = jwtService.generateToken(new UserPrincipal(user));
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    public void logout(String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        String userName = jwtService.extractUsername(token);
        var user = userRepository.findByUsername(userName).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        refreshTokenService.revokeAllRefreshTokensForUser(user.getId());
    }


    @Transactional
    public void confirmEmail(String token)  {
        String email = verificationTokenService.getEmailByToken(token);

        if (email == null) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
            var user = userRepository.findByEmail(email)
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
            emailProducer.send(
                    new EmailVerificationMessage(
                            user.getId(),
                            user.getEmail(),
                            newToken
                    )
            );
    }

}
