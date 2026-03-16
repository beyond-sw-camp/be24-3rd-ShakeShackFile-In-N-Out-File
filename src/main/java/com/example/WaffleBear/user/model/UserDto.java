package com.example.WaffleBear.user.model;

import lombok.Builder;

import java.util.Map;

public class UserDto {
    public record SignupReq(String email, String name, String password) {
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

    @Builder
    public record SignupRes(Long idx, String email, String name) {
        public static SignupRes from(User entity) {
            return SignupRes.builder()
                    .idx(entity.getIdx())
                    .email(entity.getEmail())
                    .name(entity.getName())
                    .build();
        }
    }

    @Builder
    public record OAuth(String email, String name, String provider, boolean enable, String role) {
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

    public record LoginReq(String email, String name, String password) {
    }
}
