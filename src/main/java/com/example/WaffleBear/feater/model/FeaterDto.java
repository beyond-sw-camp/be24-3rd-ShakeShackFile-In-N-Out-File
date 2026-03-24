package com.example.WaffleBear.feater.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class FeaterDto {

    @Schema(description = "사용자 설정 변경 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettingsUpdateReq {
        @Schema(description = "표시 이름", example = "홍길동")
        private String displayName;
        @Schema(description = "언어 코드", example = "ko")
        private String localeCode;
        @Schema(description = "지역 코드", example = "KR")
        private String regionCode;
        @Schema(description = "마케팅 수신 동의 여부", example = "true")
        private Boolean marketingOptIn;
        @Schema(description = "프로필 비공개 여부", example = "false")
        private Boolean privateProfile;
        @Schema(description = "이메일 알림 수신 여부", example = "true")
        private Boolean emailNotification;
        @Schema(description = "보안 알림 수신 여부", example = "true")
        private Boolean securityNotification;
        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;
    }

    @Schema(description = "사용자 설정 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettingsRes {
        @Schema(description = "사용자 IDX", example = "1")
        private Long userIdx;
        @Schema(description = "이메일", example = "user@example.com")
        private String email;
        @Schema(description = "표시 이름", example = "홍길동")
        private String displayName;
        @Schema(description = "역할", example = "ROLE_USER")
        private String role;
        @Schema(description = "이메일 인증 여부", example = "true")
        private Boolean emailVerified;
        @Schema(description = "언어 코드", example = "ko")
        private String localeCode;
        @Schema(description = "지역 코드", example = "KR")
        private String regionCode;
        @Schema(description = "마케팅 수신 동의 여부", example = "false")
        private Boolean marketingOptIn;
        @Schema(description = "프로필 비공개 여부", example = "false")
        private Boolean privateProfile;
        @Schema(description = "이메일 알림 수신 여부", example = "true")
        private Boolean emailNotification;
        @Schema(description = "보안 알림 수신 여부", example = "true")
        private Boolean securityNotification;
        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;
        @Schema(description = "멤버십 코드", example = "PRO")
        private String membershipCode;
        @Schema(description = "멤버십 표시명", example = "프로 플랜")
        private String membershipLabel;
        @Schema(description = "스토리지 플랜 표시명", example = "프로 플랜")
        private String storagePlanLabel;
        @Schema(description = "전체 스토리지 용량 (바이트)", example = "10737418240")
        private Long storageQuotaBytes;
        @Schema(description = "기본 스토리지 용량 (바이트)", example = "5368709120")
        private Long storageBaseQuotaBytes;
        @Schema(description = "추가 구매 스토리지 (바이트)", example = "5368709120")
        private Long storageAddonBytes;
        @Schema(description = "가입 일시")
        private LocalDateTime joinedAt;
        @Schema(description = "설정 수정 일시")
        private LocalDateTime updatedAt;
    }
}
