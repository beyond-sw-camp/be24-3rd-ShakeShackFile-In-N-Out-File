package com.example.WaffleBear.user.model;

import lombok.Builder;

public class UserDto {
    public record SignupReq(String email, String name, String password) {
        public User toEntity() {
            return User.builder()
                    .email(email)
                    .name(name)
                    .password(password)
                    .enable(false)
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

    public record LoginReq(String email, String name, String password){}
}
