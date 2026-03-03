package com.example.WaffleBear.config;

import com.example.WaffleBear.config.Filter.JwtFilter;
import com.example.WaffleBear.config.Filter.LoginFilter;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity // Security 설정을 활성화
public class SecurityConfig {
    private final AuthenticationConfiguration configuration;
    private final LoginFilter loginFilter;
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain config(HttpSecurity http) throws Exception {
        // 1. CORS 설정 적용 (이게 빠져있었습니다!)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 2. CSRF, FormLogin 등 비활성화
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        // 3. 인가(Authorization) 설정
        http.authorizeHttpRequests(auth -> auth
                // 로그인, 회원가입 등은 누구나 접근 가능
                .requestMatchers("/user/**","/workspace/**", "/login", "/api/login", "/error","/file/**").permitAll()
                // 나머지 요청(특히 /board/save 등)은 반드시 인증 필요
                .anyRequest().authenticated()
        );

        // 4. 필터 순서 설정
        // jwtFilter가 먼저 실행되어 쿠키를 확인하고 인증 객체를 만들어야 합니다.
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        // 그 다음 로그인 필터(ID/PW 검증)가 위치합니다.
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "ATOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
