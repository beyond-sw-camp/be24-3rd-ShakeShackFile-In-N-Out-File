package com.example.WaffleBear.config.interceptor;


import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtUtil jwtUtil;
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if(request instanceof ServletServerHttpRequest serverHttpRequest) {
            HttpServletRequest httpReq = serverHttpRequest.getServletRequest();
            if (httpReq.getCookies() != null) {
                for (Cookie cookie : httpReq.getCookies()) {
                    if (cookie.getName().equals("refresh")) {
                        // JwtUtil에서 토큰 생성 및 확인하도록 리팩토링
                        Long idx = jwtUtil.getUserIdx(cookie.getValue());
                        String email = jwtUtil.getEmail(cookie.getValue());
                        String role = jwtUtil.getRole(cookie.getValue());

                        AuthUserDetails user = AuthUserDetails.builder()
                                .idx(idx)
                                .email(email)
                                .role(role)
                                .build();

                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                        attributes.put("user", authentication);

                        return true;
                    }
                }
            }

        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}