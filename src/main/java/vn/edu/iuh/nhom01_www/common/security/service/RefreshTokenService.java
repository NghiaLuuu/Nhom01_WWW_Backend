package vn.edu.iuh.nhom01_www.common.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.model.RefreshToken;
import vn.edu.iuh.nhom01_www.common.security.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(String userEmail, Long version) {
        // Strict rotation: Optional - delete old tokens for this user first
        refreshTokenRepository.deleteByUserEmail(userEmail);

        RefreshToken refreshToken = RefreshToken.builder()
                .userEmail(userEmail)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .version(version)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new CustomException("Refresh token was expired. Please make a new signin request", HttpStatus.FORBIDDEN);
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    @Transactional
    public void deleteByToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}
