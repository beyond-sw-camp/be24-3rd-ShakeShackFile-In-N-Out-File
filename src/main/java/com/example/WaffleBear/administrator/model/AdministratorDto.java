package com.example.WaffleBear.administrator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class AdministratorDto {

    @Schema(description = "관리자 대시보드 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardRes {
        @Schema(description = "사용자 요약 정보")
        private SummaryRes summary;
        @Schema(description = "요금제별 통계 목록")
        private List<PlanStatRes> planStats;
        @Schema(description = "전체 사용자 목록")
        private List<UserRes> users;
    }

    @Schema(description = "사용자 요약 통계")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryRes {
        @Schema(description = "전체 사용자 수", example = "100")
        private long totalUserCount;
        @Schema(description = "활성 사용자 수", example = "85")
        private long activeUserCount;
        @Schema(description = "정지된 사용자 수", example = "10")
        private long suspendedUserCount;
        @Schema(description = "차단된 사용자 수", example = "5")
        private long bannedUserCount;
        @Schema(description = "전체 파일 수", example = "5000")
        private long totalFileCount;
        @Schema(description = "전체 폴더 수", example = "500")
        private long totalFolderCount;
        @Schema(description = "전체 사용 용량 (바이트)", example = "53687091200")
        private long totalUsedBytes;
        @Schema(description = "전체 할당 용량 (바이트)", example = "107374182400")
        private long totalQuotaBytes;
        @Schema(description = "전체 용량 사용률 (%)", example = "50.0")
        private double overallUsagePercent;
    }

    @Schema(description = "요금제별 통계")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanStatRes {
        @Schema(description = "요금제 코드", example = "PRO")
        private String planCode;
        @Schema(description = "요금제 표시명", example = "프로 플랜")
        private String planLabel;
        @Schema(description = "해당 요금제 사용자 수", example = "30")
        private long userCount;
        @Schema(description = "전체 대비 사용자 비율 (%)", example = "30.0")
        private double userPercent;
        @Schema(description = "사용 용량 (바이트)", example = "16106127360")
        private long usedBytes;
        @Schema(description = "할당 용량 (바이트)", example = "32212254720")
        private long quotaBytes;
        @Schema(description = "용량 사용률 (%)", example = "50.0")
        private double usagePercent;
    }

    @Schema(description = "관리자용 사용자 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserRes {
        @Schema(description = "사용자 IDX", example = "1")
        private Long idx;
        @Schema(description = "사용자 ID (이메일)", example = "user@example.com")
        private String id;
        @Schema(description = "사용자 이름", example = "홍길동")
        private String name;
        @Schema(description = "역할", example = "ROLE_USER")
        private String role;
        @Schema(description = "계정 상태", example = "ACTIVE")
        private String accountStatus;
        @Schema(description = "계정 활성화 여부", example = "true")
        private boolean enabled;
        @Schema(description = "요금제 코드", example = "FREE")
        private String planCode;
        @Schema(description = "요금제 표시명", example = "무료 플랜")
        private String planLabel;
        @Schema(description = "사용 용량 (바이트)", example = "536870912")
        private long usedBytes;
        @Schema(description = "할당 용량 (바이트)", example = "5368709120")
        private long quotaBytes;
        @Schema(description = "용량 사용률 (%)", example = "10.0")
        private double usagePercent;
        @Schema(description = "파일 수", example = "50")
        private long fileCount;
        @Schema(description = "폴더 수", example = "10")
        private long folderCount;
        @Schema(description = "공유 파일 수", example = "5")
        private long sharedFileCount;
    }

    @Schema(description = "사용자 상태 변경 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateReq {
        @Schema(description = "변경할 계정 상태 (ACTIVE/SUSPENDED/BANNED)", example = "SUSPENDED")
        private String accountStatus;
    }

    @Schema(description = "스토리지 분석 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageAnalyticsRes {
        @Schema(description = "분석 기간 정보")
        private StorageAnalyticsWindowRes window;
        @Schema(description = "스토리지 요약 정보")
        private StorageSummaryRes summary;
        @Schema(description = "스토리지 무결성 정보")
        private StorageIntegrityRes integrity;
        @Schema(description = "스토리지 소스별 사용 현황")
        private List<StorageBreakdownRes> storageBreakdown;
        @Schema(description = "전송 내역 분류")
        private List<TransferBreakdownRes> transferBreakdown;
        @Schema(description = "사용자별 전송 통계")
        private List<UserTransferStatRes> users;
    }

    @Schema(description = "스토리지 요약 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageSummaryRes {
        @Schema(description = "스토리지 전체 용량 (바이트)", example = "1099511627776")
        private long providerCapacityBytes;
        @Schema(description = "스토리지 사용 용량 (바이트)", example = "549755813888")
        private long providerUsedBytes;
        @Schema(description = "스토리지 남은 용량 (바이트)", example = "549755813888")
        private long providerRemainingBytes;
        @Schema(description = "스토리지 사용률 (%)", example = "50.0")
        private double providerUsagePercent;
        @Schema(description = "사용자 할당 총 용량 (바이트)", example = "107374182400")
        private long allocatedUserQuotaBytes;
        @Schema(description = "사용자 실사용 총 용량 (바이트)", example = "53687091200")
        private long allocatedUserUsedBytes;
        @Schema(description = "사용자 할당 대비 사용률 (%)", example = "50.0")
        private double allocatedUserUsagePercent;
        @Schema(description = "전체 인그레스 (바이트)", example = "10737418240")
        private long totalIngressBytes;
        @Schema(description = "완료된 인그레스 (바이트)", example = "9663676416")
        private long completedIngressBytes;
        @Schema(description = "취소된 인그레스 (바이트)", example = "1073741824")
        private long canceledIngressBytes;
        @Schema(description = "전체 이그레스 (바이트)", example = "5368709120")
        private long totalEgressBytes;
        @Schema(description = "추적 대상 사용자 수", example = "100")
        private long trackedUserCount;
    }

    @Schema(description = "분석 기간 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageAnalyticsWindowRes {
        @Schema(description = "기간 코드", example = "30d")
        private String rangeCode;
        @Schema(description = "기간 표시명", example = "최근 30일")
        private String rangeLabel;
        @Schema(description = "시작 일시")
        private LocalDateTime startedAt;
        @Schema(description = "종료 일시")
        private LocalDateTime endedAt;
    }

    @Schema(description = "스토리지 무결성 검사 결과")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageIntegrityRes {
        @Schema(description = "정상 여부", example = "true")
        private boolean healthy;
        @Schema(description = "문제 건수", example = "0")
        private int issueCount;
        @Schema(description = "문제 목록")
        private List<String> issues;
        @Schema(description = "드라이브 예약 대기 용량 (바이트)", example = "0")
        private long pendingDriveReservationBytes;
        @Schema(description = "검사 일시")
        private LocalDateTime generatedAt;
    }

    @Schema(description = "소스별 스토리지 사용 현황")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageBreakdownRes {
        @Schema(description = "소스 코드", example = "USER_FILES")
        private String source;
        @Schema(description = "소스 표시명", example = "사용자 파일")
        private String label;
        @Schema(description = "저장 용량 (바이트)", example = "53687091200")
        private long storedBytes;
    }

    @Schema(description = "전송 내역 분류")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferBreakdownRes {
        @Schema(description = "전송 방향 (INGRESS/EGRESS)", example = "INGRESS")
        private String direction;
        @Schema(description = "소스 코드", example = "UPLOAD")
        private String source;
        @Schema(description = "소스 표시명", example = "파일 업로드")
        private String label;
        @Schema(description = "상태", example = "COMPLETED")
        private String status;
        @Schema(description = "전송량 (바이트)", example = "1073741824")
        private long bytes;
        @Schema(description = "이벤트 건수", example = "100")
        private long eventCount;
    }

    @Schema(description = "사용자별 전송 통계")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserTransferStatRes {
        @Schema(description = "사용자 IDX", example = "1")
        private Long idx;
        @Schema(description = "사용자 ID (이메일)", example = "user@example.com")
        private String id;
        @Schema(description = "사용자 이름", example = "홍길동")
        private String name;
        @Schema(description = "요금제 코드", example = "PRO")
        private String planCode;
        @Schema(description = "요금제 표시명", example = "프로 플랜")
        private String planLabel;
        @Schema(description = "할당 용량 (바이트)", example = "10737418240")
        private long quotaBytes;
        @Schema(description = "현재 저장 용량 (바이트)", example = "5368709120")
        private long currentStoredBytes;
        @Schema(description = "전체 인그레스 (바이트)", example = "1073741824")
        private long totalIngressBytes;
        @Schema(description = "완료된 인그레스 (바이트)", example = "966367641")
        private long completedIngressBytes;
        @Schema(description = "취소된 인그레스 (바이트)", example = "107374182")
        private long canceledIngressBytes;
        @Schema(description = "전체 이그레스 (바이트)", example = "536870912")
        private long totalEgressBytes;
    }

    @Schema(description = "스토리지 용량 변경 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageCapacityUpdateReq {
        @Schema(description = "변경할 전체 용량 (바이트)", example = "1099511627776")
        private Long providerCapacityBytes;
    }
}
