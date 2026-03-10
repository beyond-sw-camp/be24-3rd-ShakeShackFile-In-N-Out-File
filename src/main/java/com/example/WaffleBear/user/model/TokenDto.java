package com.example.WaffleBear.user.model;

public class TokenDto {
    public record AuthTokenResponse(
            String accessToken,
            String refreshToken
    ) {}
}
