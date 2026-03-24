package com.example.WaffleBear.user.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenDto {
    @Schema(description = "인증 토큰 응답")
    public record AuthTokenResponse(
            @Schema(description = "JWT Access Token (API 요청 시 Authorization 헤더에 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
            String accessToken,
            @Schema(description = "JWT Refresh Token (토큰 재발급 시 사용, HttpOnly 쿠키로 저장)", example = "eyJhbGciOiJIUzI1NiJ9...")
            String refreshToken
    ) {}
}
