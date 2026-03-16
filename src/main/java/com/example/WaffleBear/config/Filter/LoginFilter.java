package com.example.WaffleBear.config.Filter;

import com.example.WaffleBear.user.model.TokenDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.UserDto;
import com.example.WaffleBear.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;



@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public LoginFilter(
            AuthenticationManager authenticationManager,
            AuthService authService) {

        super(authenticationManager);
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult) throws IOException, ServletException {

        AuthUserDetails user = (AuthUserDetails) authResult.getPrincipal();

        // 1. 서비스 계층에 비즈니스 로직 위임
        TokenDto.AuthTokenResponse tokens = authService.issueTokens(
                user.getIdx(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        // 2. HTTP 응답 제어 (Access Token -> Header)
        response.setHeader("Authorization", "Bearer " + tokens.accessToken());

        // 3. HTTP 응답 제어 (Refresh Token -> HttpOnly Cookie)
        Cookie refreshCookie = new Cookie("refresh", tokens.refreshToken());
        refreshCookie.setMaxAge(14 * 24 * 60 * 6000000);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        // refreshCookie.setSecure(true); // 운영 환경(HTTPS)에서는 필수 활성화
        response.addCookie(refreshCookie);
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        response.getWriter().write("로그인 실패");
    }

    // TODO : 1번
    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {

        System.out.println("필터 실행됌.");

        try {
            UserDto.LoginReq dto = new ObjectMapper().readValue(
                    request.getInputStream(),
                    UserDto.LoginReq.class);

            // TODO : 2번
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(
                            dto.email(),
                            dto.password(),
                            null);
            // TODO : 3번
            return authenticationManager.authenticate(token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
