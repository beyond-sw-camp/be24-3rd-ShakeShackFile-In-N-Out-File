package com.example.WaffleBear.config.Filter;

import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserAccountStatus;
import com.example.WaffleBear.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/login")
                || path.startsWith("/auth/reissue")
                || path.startsWith("/user/signup")
                || path.startsWith("/user/verify")
                || path.startsWith("/oauth2")
                || path.startsWith("/login/oauth2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.split(" ")[1];

        try {
            jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "access token expired");
            return;
        }

        String category = jwtUtil.getCategory(token);
        if (!"access".equals(category)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid token category");
            return;
        }

        Long idx = jwtUtil.getUserIdx(token);
        User userEntity = userRepository.findById(idx).orElse(null);
        if (userEntity == null || !Boolean.TRUE.equals(userEntity.getEnable()) || resolveStatus(userEntity) != UserAccountStatus.ACTIVE) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "user access blocked");
            return;
        }

        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);
        String id = jwtUtil.getId(token);
        if (id == null || id.isBlank()) {
            id = email;
        }

        AuthUserDetails user = AuthUserDetails.builder()
                .idx(idx)
                .id(id)
                .email(email)
                .role(role)
                .name(userEntity.getName())
                .enable(userEntity.getEnable())
                .accountStatus(resolveStatus(userEntity))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private UserAccountStatus resolveStatus(User user) {
        return user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print("{\"error\": \"" + message + "\"}");
    }
}
