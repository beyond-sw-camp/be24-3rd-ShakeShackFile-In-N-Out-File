package com.example.WaffleBear.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private SecretKey encodeKey;

    // 더 이상 사용하지 않는 expire 필드는 삭제함

    public JwtUtil(@Value("${jwt.key}") String key) {
        this.encodeKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(key));
    }

    public String createToken(String category, Long idx, String email, String name, String role, Long expiredMs) {
        return Jwts.builder()
                .claim("category", category) // "access" 또는 "refresh"
                .claim("idx", idx)
                .claim("email", email)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(new Date(System.currentTimeMillis()))
                // 치명적 버그 수정: 주입받은 expire 대신 파라미터로 받은 expiredMs 사용
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(encodeKey)
                .compact();
    }

    public Long getUserIdx(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("idx", Long.class);
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    public String getName(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("name", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    // 신규 추가: 토큰 카테고리(access/refresh) 추출 메서드
    public String getCategory(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("category", String.class);
    }

    // 신규 추가: 토큰 만료 여부 검증 메서드
    public Boolean isExpired(String token) {
        return Jwts.parser()
                .verifyWith(encodeKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }
}