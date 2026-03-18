package com.example.WaffleBear.user.service;


import com.example.WaffleBear.user.model.RefreshToken;
import com.example.WaffleBear.user.model.TokenDto;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserAccountStatus;
import com.example.WaffleBear.user.repository.RefreshTokenRepository;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.utils.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

        // 2. DB 영속성 관리 (Upsert: 존재하면 갱신, 없으면 삽입)
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

        // 3. 결과 래핑 후 반환
        return new TokenDto.AuthTokenResponse(access, refresh);
    }

    @Transactional
    public TokenDto.AuthTokenResponse reissue(String refreshToken) {
        // 1. 토큰 존재 여부 확인
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }

        validateRefreshToken(refreshToken);

        // 3. 토큰 카테고리 검증 (Refresh Token이 맞는지 확인)
        String category = jwtUtil.getCategory(refreshToken);
        if (!category.equals("refresh")) {
            throw new IllegalArgumentException("유효하지 않은 토큰 카테고리입니다.");
        }

        // 4. DB에 저장된 토큰과 대조 (핵심 보안 로직)
        String email = jwtUtil.getEmail(refreshToken);
        RefreshToken dbToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("DB에 등록되지 않은 사용자/토큰입니다."));

        if (!dbToken.getToken().equals(refreshToken)) {
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다. 탈취가 의심됩니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!Boolean.TRUE.equals(user.getEnable()) || resolveStatus(user) != UserAccountStatus.ACTIVE) {
            refreshTokenRepository.deleteByEmail(email);
            throw new IllegalArgumentException("User is not allowed to access.");
        }

        String resolvedUserId = jwtUtil.getId(refreshToken);
        if (resolvedUserId == null || resolvedUserId.isBlank()) {
            resolvedUserId = user.getEmail();
        }

        String newAccess = jwtUtil.createToken(
                "access",
                user.getIdx(),
                resolvedUserId,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                600000L
        );
        String newRefresh = jwtUtil.createToken(
                "refresh",
                user.getIdx(),
                resolvedUserId,
                user.getEmail(),
                user.getName(),
                user.getRole(),
                1209600000L
        );

        dbToken.updateToken(newRefresh, LocalDateTime.now().plusDays(14));

        return new TokenDto.AuthTokenResponse(newAccess, newRefresh);
    }

    private UserAccountStatus resolveStatus(User user) {
        return user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
    }

    private void validateRefreshToken(String refreshToken) {
        try {
            if (Boolean.TRUE.equals(jwtUtil.isExpired(refreshToken))) {
                throw new IllegalArgumentException("Refresh Token이 만료되었습니다.");
            }
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Refresh Token이 만료되었습니다.");
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByToken(refreshToken);
    }
}
