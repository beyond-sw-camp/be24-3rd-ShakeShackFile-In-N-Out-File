// OAuth2AuthenticationSuccessHandler.java
package com.example.WaffleBear.config.oauth2;

import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.TokenDto;
import com.example.WaffleBear.user.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // JwtUtil 대신 AuthService 주입 (발급 및 DB 저장 로직 재사용)
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        AuthUserDetails user = (AuthUserDetails) authentication.getPrincipal();

        // 1. 토큰 발급 및 Refresh Token DB 저장 (로컬 로그인과 동일한 규격)
        TokenDto.AuthTokenResponse tokens = authService.issueTokens(user.getIdx(), user.getEmail(), user.getName(), user.getRole());

        // 2. Refresh Token은 HttpOnly 쿠키로 설정
        Cookie refreshCookie = new Cookie("refresh", tokens.refreshToken());
        refreshCookie.setMaxAge(14 * 24 * 60 * 60);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        // refreshCookie.setSecure(true); // 운영 환경 도입 시 주석 해제
        response.addCookie(refreshCookie);

        // 3. Access Token은 Redirect URL의 쿼리 파라미터로 전달
        String redirectUrl = "http://localhost:5173/main?accessToken=" + tokens.accessToken();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}