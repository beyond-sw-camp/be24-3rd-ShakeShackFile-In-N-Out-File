package com.example.WaffleBear.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BaseResponseStatus {
    // 2000번대 성공
    SUCCESS(true, 2000, "요청이 성공했습니다"),

    // 3000번대 클라이언트 입력 오류, 입력값 검증 오류
    JWT_EXPIRED(false, 3001, "JWT 토큰이 만료되었습니다."),
    JWT_INVALID(false, 3002, "JWT 토큰이 유효하지 않습니다."),
    SIGNUP_DUPLICATE_EMAIL(false, 3003, "중복된 이메일입니다."),
    SIGNUP_INVALID_PASSWORD(false, 3004, "비밀번호는 대,소문자, 숫자, 특수문자가 포함되어야 합니다."),
    SIGNUP_INVALID_UUID(false, 3005, "유효하지 않은 인증값입니다. 이메일 인증을 다시 시도해주세요."),
    LOGIN_INVALID_USERINFO(false, 3006, "이메일이나 비밀번호를 확인해주세요."),

    // 4000번대
    REQUEST_ERROR(false, 4001, "입력값이 잘못되었습니다."),


    // 5000번대 실패
    FAIL(false, 5000, "요청이 실패했습니다");

    private final boolean success;
    private final int code;
    private final String message;
}
