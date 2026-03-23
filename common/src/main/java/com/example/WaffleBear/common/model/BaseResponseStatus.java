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

    REQUEST_ERROR(false, 4001, "입력값이 올바르지 않습니다."),

    FAIL(false, 5000, "요청 처리에 실패했습니다."),

    INVALID_EMAIL_FORMAT(false, 6001, "이메일 형식이 올바르지 않습니다."),

    USER_NOT_FOUND(false, 6002, "해당 사용자가 존재하지 않습니다."),
    USER_NOT_REGISTERED(false, 6003, "가입되지 않은 이메일입니다. 회원가입을 먼저 진행해 주세요."),

    WORKSPACE_NOT_FOUND(false, 6010, "해당 워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_ACCESS_DENIED(false, 6011, "워크스페이스 접근 권한이 없습니다."),
    WORKSPACE_SHARE_NOT_ALLOWED(false, 6012, "이 워크스페이스는 공유할 수 없습니다."),
    WORKSPACE_SHARE_ENDED(false, 6013, "워크스페이스 공유가 종료되었습니다."),
    WORKSPACE_NOT_ACCESSIBLE(false, 6014, "접근 가능한 워크스페이스가 아닙니다."),

    EMAIL_VERIFY_TOKEN_INVALID(false, 6020, "유효하지 않은 토큰입니다."),
    EMAIL_VERIFY_TOKEN_EXPIRED(false, 6021, "토큰이 만료되었습니다."),
    INVITE_REJECTED(false, 6022, "초대를 거절했습니다."),
    ALREADY_JOINED(false, 6023, "이미 참여 중인 사용자입니다."),

    ROLE_SAVE_FAIL(false, 6030, "권한 수정에 실패했습니다."),
    ROLE_LOAD_FAIL(false, 6031, "권한 조회에 실패했습니다."),
    ADMIN_ONLY_ACTION(false, 6032, "관리자만 권한을 변경할 수 있습니다."),
    GROUP_RELATIONSHIP_NOT_FOUND(false, 6033, "관계를 찾을 수 없습니다."),
    GROUP_NOT_FOUND(false, 6034, "그룹을 찾을 수 없습니다."),
    GROUP_INVITE_NOT_FOUND(false, 6035, "그룹 초대를 찾을 수 없습니다."),
    GROUP_INVITE_ALREADY_PROCESSED(false, 6036, "이미 처리된 그룹 초대입니다."),
    RELATIONSHIP_INVITE_NOT_FOUND(false, 6037, "초대를 찾을 수 없습니다."),
    RELATIONSHIP_INVITE_ALREADY_PROCESSED(false, 6038, "이미 처리된 초대입니다."),
    DUPLICATE_RELATIONSHIP(false, 6039, "이미 연결된 사용자입니다."),
    GROUP_NAME_DUPLICATED(false, 6040, "같은 이름의 그룹이 이미 존재합니다."),
    GROUP_ACCESS_DENIED(false, 6041, "그룹 접근 권한이 없습니다."),
    RELATIONSHIP_REQUIRED(false, 6042, "관계가 형성된 사용자만 그룹에 추가할 수 있습니다.");

    private final boolean success;
    private final int code;
    private final String message;
}
