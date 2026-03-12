package com.example.WaffleBear.user.service;

import com.example.WaffleBear.user.model.RefreshToken;
import com.example.WaffleBear.user.model.TokenDto;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserAccountStatus;
import com.example.WaffleBear.user.repository.RefreshTokenRepository;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public TokenDto.AuthTokenResponse issueTokens(Long userIdx, String userId, String email, String name, String role) {
        String resolvedUserId = (userId == null || userId.isBlank()) ? email : userId;
        String access = jwtUtil.createToken("access", userIdx, resolvedUserId, email, name, role, 600000L);
        String refresh = jwtUtil.createToken("refresh", userIdx, resolvedUserId, email, name, role, 1209600000L);
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);

        refreshTokenRepository.findByEmail(email)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refresh, expiryDate),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .email(email)
                                        .token(refresh)
                                        .expiryDate(expiryDate)
                                        .build()
                        )
                );

        return new TokenDto.AuthTokenResponse(access, refresh);
    }

    @Transactional
    public TokenDto.AuthTokenResponse reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token is required.");
        }

        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Refresh token expired.");
        }

        String category = jwtUtil.getCategory(refreshToken);
        if (!"refresh".equals(category)) {
            throw new IllegalArgumentException("Invalid refresh token.");
        }

        String email = jwtUtil.getEmail(refreshToken);
        RefreshToken dbToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found."));

        if (!dbToken.getToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh token mismatch.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!Boolean.TRUE.equals(user.getEnable()) || resolveStatus(user) != UserAccountStatus.ACTIVE) {
            refreshTokenRepository.deleteByEmail(email);
            throw new IllegalArgumentException("User is not allowed to access.");
        }

        String resolvedUserId = user.getEmail();

        String newAccess = jwtUtil.createToken("access", user.getIdx(), resolvedUserId, email, user.getName(), user.getRole(), 600000L);
        String newRefresh = jwtUtil.createToken("refresh", user.getIdx(), resolvedUserId, email, user.getName(), user.getRole(), 1209600000L);

        dbToken.updateToken(newRefresh, LocalDateTime.now().plusDays(14));
        return new TokenDto.AuthTokenResponse(newAccess, newRefresh);
    }

    private UserAccountStatus resolveStatus(User user) {
        return user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
    }
}
