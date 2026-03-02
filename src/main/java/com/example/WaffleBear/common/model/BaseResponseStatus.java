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

    FILE_NAME_WRONG(false, 3501, "파일의 이름이 정상적이지 않습니다. 수정하셔야 업로드가 가능합니다."),
    FILE_FORMAT_WRONG(false, 3502, "파일의 형식이 지원하지 않는 형식입니다. 해당 형식은 지원하지 않습니다."),
    FILE_SIZE_WRONG(false, 3503, "파일의 크기가 지원하는 범위를 벗어났습니다. 해당 크기는 지원하지 않습니다."),
    FILE_COUNT_WRONG(false, 3504, "파일의 업로드 개수가 지원하는 범위를 벗어났습니다. 정해진 개수 이하로 업로드 하십시오."),
    FILE_UPLOAD_TIMEOUT(false, 3505, "파일 업로드의 최대 시간이 만료되었습니다. 다시 시도하십시오."),
    FILE_DOWNLOAD_TIMEOUT(false, 3506, "파일 다운로드의 최대 시간이 만료되었습니다. 다시 시도하십시오."),
    FILE_UPDATE_TIMEOUT(false, 3507, "파일 조회에 실패하였습니다. 다시 시도하십시오."),

    // 4000번대
    REQUEST_ERROR(false, 4001, "입력값이 잘못되었습니다."),



    // 5000번대 실패
    FAIL(false, 5000, "요청이 실패했습니다");

    private final boolean success;
    private final int code;
    private final String message;
}
