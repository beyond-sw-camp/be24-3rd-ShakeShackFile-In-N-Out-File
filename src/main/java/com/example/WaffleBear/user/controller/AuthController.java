package com.example.WaffleBear.user.controller;

import com.example.WaffleBear.user.model.TokenDto;
import com.example.WaffleBear.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "인증 (Auth)", description = "JWT 토큰 재발급 및 로그아웃 등 인증 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.secure-cookie}")
    private boolean secureCookie;

    @Operation(summary = "토큰 재발급", description = "쿠키에 저장된 Refresh Token으로 새로운 Access Token과 Refresh Token을 재발급합니다. "
            + "재발급된 Access Token은 Authorization 헤더에, Refresh Token은 HttpOnly 쿠키에 설정됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "400", description = "Refresh Token이 없거나 유효하지 않음"),
            @ApiResponse(responseCode = "401", description = "만료되었거나 탈취가 의심되는 토큰")
    })
    @PostMapping("/reissue")
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

    @Operation(summary = "로그아웃", description = "Refresh Token을 DB에서 삭제하고 쿠키를 초기화하여 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
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
