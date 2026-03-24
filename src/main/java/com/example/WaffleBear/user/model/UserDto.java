package com.example.WaffleBear.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

public class UserDto {
    @Schema(description = "회원가입 요청")
    public record SignupReq(
            @Schema(description = "이메일 주소", example = "user@example.com") String email,
            @Schema(description = "사용자 이름", example = "홍길동") String name,
            @Schema(description = "비밀번호 (영문+숫자+특수문자 조합 권장)", example = "Qwer1234%") String password
    ) {
        public User toEntity() {
            return User.builder()
                    .email(email)
                    .name(name)
                    .password(password)
                    .enable(false)
                    .role("ROLE_USER")
                    .accountStatus(UserAccountStatus.ACTIVE)
                    .build();
        }
    }

    @Schema(description = "회원가입 응답")
    @Builder
    public record SignupRes(
            @Schema(description = "사용자 고유 번호", example = "1") Long idx,
            @Schema(description = "이메일 주소", example = "user@example.com") String email,
            @Schema(description = "사용자 이름", example = "홍길동") String name
    ) {
        public static SignupRes from(User entity) {
            return SignupRes.builder()
                    .idx(entity.getIdx())
                    .email(entity.getEmail())
                    .name(entity.getName())
                    .build();
        }
    }

    @Schema(description = "OAuth2 소셜 로그인 정보")
    @Builder
    public record OAuth(
            @Schema(description = "이메일 주소") String email,
            @Schema(description = "사용자 이름") String name,
            @Schema(description = "OAuth 제공자 (google/naver/kakao)") String provider,
            @Schema(description = "계정 활성화 여부") boolean enable,
            @Schema(description = "사용자 권한") String role
    ) {
        public static OAuth from(Map<String, Object> attributes, String provider) {
            if (provider.equals("google")) {
                String email = (String) attributes.get("email");
                String name = (String) attributes.get("name");
                return OAuth.builder()
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .enable(true)
                        .role("ROLE_USER")
                        .build();
            } else if (provider.equals("kakao")) {
                String providerId = ((Long) attributes.get("id")).toString();
                String email = providerId + "@kakao.social";
                Map properties = (Map) attributes.get("properties");
                String name = (String) properties.get("nickname");

                return OAuth.builder()
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .enable(true)
                        .role("ROLE_USER")
                        .build();
            } else if (provider.equals("naver")) {
                Map response = (Map) attributes.get("response");
                String email = (String) response.get("email");
                String name = (String) response.get("name");
                return OAuth.builder()
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .enable(true)
                        .role("ROLE_USER")
                        .build();
            }
            return null;
        }

        public User toEntity() {
            return User.builder()
                    .email(email)
                    .name(name)
                    .password(provider)
                    .enable(enable)
                    .role(role)
                    .accountStatus(UserAccountStatus.ACTIVE)
                    .build();
        }
    }

    @Schema(description = "로그인 요청")
    public record LoginReq(
            @Schema(description = "이메일 주소", example = "user@example.com") String email,
            @Schema(description = "사용자 이름 (선택)", example = "홍길동") String name,
            @Schema(description = "비밀번호", example = "Qwer1234%") String password
    ) {
    }
}
