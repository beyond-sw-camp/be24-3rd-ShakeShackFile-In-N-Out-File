package com.example.WaffleBear.user.controller;

import com.example.WaffleBear.user.model.TokenDto;
import com.example.WaffleBear.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Token lifecycle APIs")
public class AuthController {

    private final AuthService authService;

    @Value("${app.secure-cookie}")
    private boolean secureCookie;

    @PostMapping("/reissue")
    @Operation(summary = "Reissue token", description = "Reissues access and refresh tokens using the refresh cookie.")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        TokenDto.AuthTokenResponse tokens = authService.reissue(refreshToken);

        response.setHeader("Authorization", "Bearer " + tokens.accessToken());

        Cookie refreshCookie = new Cookie("refresh", tokens.refreshToken());
        refreshCookie.setMaxAge(14 * 24 * 60 * 60);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setSecure(secureCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok().body("토큰 재발급 완료");
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidates the refresh token and clears the refresh cookie.")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        Cookie refreshCookie = new Cookie("refresh", null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setSecure(secureCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok().body("로그아웃 완료");
    }
}
