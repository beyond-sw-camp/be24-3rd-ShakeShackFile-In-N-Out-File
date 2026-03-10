package com.example.WaffleBear.user.service;


import com.example.WaffleBear.user.repository.RefreshTokenRepository;
import com.example.WaffleBear.user.model.RefreshToken;
import com.example.WaffleBear.user.model.TokenDto;
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

    @Transactional
    public TokenDto.AuthTokenResponse issueTokens(Long userId, String email, String name, String role) {
        // 1. 토큰 생성 로직
        String access = jwtUtil.createToken("access", userId, email, name, role, 600000L); // 10분
        String refresh = jwtUtil.createToken("refresh", userId, email, name, role, 1209600000L); // 14일
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(14);

        // 2. DB 영속성 관리 (Upsert: 존재하면 갱신, 없으면 삽입)
        refreshTokenRepository.findByEmail(email)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refresh, expiryDate),
                        () -> {
                            RefreshToken newToken = RefreshToken.builder()
                                    .email(email)
                                    .token(refresh)
                                    .expiryDate(expiryDate)
                                    .build();
                            refreshTokenRepository.save(newToken);
                        }
                );

        // 3. 결과 래핑 후 반환
        return new TokenDto.AuthTokenResponse(access, refresh);
    }

    @Transactional
    public TokenDto.AuthTokenResponse reissue(String refreshToken) {
        // 1. 토큰 존재 여부 확인
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }

        // 2. 토큰 만료 여부 검증 (JwtUtil에 isExpired 메서드가 있다고 가정)
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Refresh Token이 만료되었습니다.");
        }

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

        // 5. 새로운 토큰 정보 추출 및 생성
        Long userId = jwtUtil.getUserIdx(refreshToken);
        String name = jwtUtil.getName(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String newAccess = jwtUtil.createToken("access", userId, email, name, role, 600000L); // 10분
        String newRefresh = jwtUtil.createToken("refresh", userId, email, name, role, 1209600000L); // 14일

        // 6. DB의 기존 Refresh Token 갱신 (RTR 기법)
        dbToken.updateToken(newRefresh, LocalDateTime.now().plusDays(14));

        return new TokenDto.AuthTokenResponse(newAccess, newRefresh);
    }
}
