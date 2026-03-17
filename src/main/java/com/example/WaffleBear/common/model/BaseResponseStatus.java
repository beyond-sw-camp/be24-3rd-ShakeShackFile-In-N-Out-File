package com.example.WaffleBear.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BaseResponseStatus {
    SUCCESS(true, 2000, "요청이 성공했습니다."),

    JWT_EXPIRED(false, 3001, "JWT 토큰이 만료되었습니다."),
    JWT_INVALID(false, 3002, "JWT 토큰이 유효하지 않습니다."),
    SIGNUP_DUPLICATE_EMAIL(false, 3003, "중복된 이메일입니다."),
    SIGNUP_DUPLICATE_NAME(false, 3004, "중복된 이름입니다."),
    SIGNUP_INVALID_PASSWORD(false, 3005, "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."),
    SIGNUP_INVALID_UUID(false, 3006, "유효하지 않은 인증값입니다. 이메일 인증을 다시 시도해 주세요."),
    LOGIN_INVALID_USERINFO(false, 3007, "이메일 또는 비밀번호를 확인해 주세요."),

    FILE_NAME_WRONG(false, 3501, "파일 이름이 올바르지 않습니다."),
    FILE_FORMAT_WRONG(false, 3502, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_WRONG(false, 3503, "현재 멤버십의 파일 업로드 크기 제한을 초과했습니다."),
    FILE_COUNT_WRONG(false, 3504, "한 번에 업로드할 수 있는 파일 개수를 초과했습니다."),
    FILE_UPLOAD_TIMEOUT(false, 3505, "파일 업로드 시간이 초과되었습니다."),
    FILE_DOWNLOAD_TIMEOUT(false, 3506, "파일 다운로드 시간이 초과되었습니다."),
    FILE_UPDATE_TIMEOUT(false, 3507, "파일 조회에 실패했습니다."),
    FILE_EMPTY(false, 3508, "요청한 파일 정보가 없습니다."),
    FILE_NAME_LENGTH_WRONG(false, 3509, "파일 이름 길이가 허용 범위를 초과했습니다."),
    FILE_FORMAT_NOTHING(false, 3510, "파일 확장자가 없습니다."),
    FILE_UPLOADURL_FAIL(false, 3511, "업로드 URL 처리에 실패했습니다."),
    STORAGE_QUOTA_EXCEEDED(false, 3512, "저장 공간이 부족합니다. 용량을 정리하거나 추가 저장용량을 구매해 주세요."),
    PLAN_FEATURE_NOT_AVAILABLE(false, 3513, "현재 멤버십에서 지원하지 않는 기능입니다."),
    PROFILE_IMAGE_SIZE_OVER(false, 3514, "프로파일의 사이즈가 지원 범위를 벗어 났습니다."),

    REQUEST_ERROR(false, 4001, "입력값이 올바르지 않습니다."),
    FAIL(false, 5000, "요청 처리에 실패했습니다."),
    INVALID_EMAIL_FORMAT(false, 6001, "이메일 형식이 올바르지 않습니다.");

    private final boolean success;
    private final int code;
    private final String message;
}