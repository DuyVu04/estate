package com.project.estate.security.service;

import com.project.estate.entity.RefreshToken;
import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.repository.RefreshTokenRepository;
import com.project.estate.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    @Transactional
    public RefreshToken createRefreshToken(String userId){
        var user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser_Id(userId);
        return refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                        .revoked(false)
                        .build()
        );
    }

    public Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }

    public void verifyExpiration(RefreshToken token){
        if (token.getExpiryDate().isBefore(Instant.now())) {

            token.setRevoked(true);

            refreshTokenRepository.save(token);

            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);

        }
    }

    @Transactional
    public void revokeAllRefreshTokensForUser(String userId) {
        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findAllByUserIdAndRevokedFalse(userId);

        tokens.forEach(token -> token.setRevoked(true));

        refreshTokenRepository.saveAll(tokens);
    }

    @Transactional
    public void revoke(RefreshToken token) {

        token.setRevoked(true);

        refreshTokenRepository.save(token);

    }

    @Transactional
    public RefreshToken rotate(RefreshToken oldToken) {

        oldToken.setRevoked(true);

        refreshTokenRepository.save(oldToken);

        return createRefreshToken(
                oldToken.getUser().getId()
        );

    }

}
