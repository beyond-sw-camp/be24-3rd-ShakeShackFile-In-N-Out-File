package com.example.WaffleBear.config.Filter;

import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // 로그인, 회원가입, 재발급 등 토큰 검증이 필요 없는 경로는 필터를 건너뜀
        return path.startsWith("/login") ||
                path.startsWith("/auth/reissue") ||
                path.startsWith("/user/signup") ||
                path.startsWith("/user/verify") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/login/oauth2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 헤더에서 Authorization 키를 찾음
        String authorization = request.getHeader("Authorization");

        // 2. Authorization 헤더가 없거나 Bearer 접두사가 아니면 검증 종료 (다음 필터로)
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("DEBUG: Authorization 헤더가 없거나 형식이 틀림: " + authorization);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 부분을 제거하고 순수 토큰 문자열만 추출
        String token = authorization.substring(7).trim();

        // ★ 이 로그가 핵심입니다. 서버 콘솔(Log)에 뭐라고 찍히는지 확인해 보세요!
        System.out.println("DEBUG: 추출된 순수 토큰 -> [" + token + "]");

        // 4. 토큰 검증
        try {
            jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            // 프론트엔드가 토큰 만료를 인지하고 재발급 API를 호출할 수 있도록 401 응답
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.print("{\"error\": \"access token expired\"}");
            return;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.print("{\"error\": \"invalid token\"}");
            return;
        }

        // 5. 토큰 카테고리 검증 (access 토큰이 맞는지)
        String category = jwtUtil.getCategory(token);
        if (!category.equals("access")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.print("{\"error\": \"invalid token category\"}");
            return;
        }

        // 6. 정상 토큰이면 값 추출 후 SecurityContextHolder에 인증 정보 저장
        Long idx = jwtUtil.getUserIdx(token);
        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);

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
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}