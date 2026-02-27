package com.example.WaffleBear.Config.Filter;


import com.example.WaffleBear.Utils.JwtUtil;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, JwtUtil JwtUtil) {
        super(authenticationManager);
        this.authenticationManager = authenticationManager;
        this.jwtUtil = JwtUtil;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        System.out.println("로그인 성공했을 때 실행");
        AuthUserDetails user = (AuthUserDetails) authResult.getPrincipal();
        String token = jwtUtil.createToken(user.getIdx(), user.getUsername(), user.getRole());
        response.setHeader("Set-Cookie", "ATOKEN=" + token + "; Path=/");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.getWriter().write("login failed");
    }

    // TODO : 1번
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("내 필터가 실행되는데요??????????");

        try {
            UserDto.LoginReq dto = new ObjectMapper().readValue(request.getInputStream(), UserDto.LoginReq.class);

            // TODO : 2번
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.password(), null);
            // TODO : 3번
            return authenticationManager.authenticate(token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}