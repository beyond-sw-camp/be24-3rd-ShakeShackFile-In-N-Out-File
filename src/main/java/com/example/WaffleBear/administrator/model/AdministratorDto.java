package com.example.WaffleBear.administrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class AdministratorDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardRes {
        private SummaryRes summary;
        private List<PlanStatRes> planStats;
        private List<UserRes> users;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryRes {
        private long totalUserCount;
        private long activeUserCount;
        private long suspendedUserCount;
        private long bannedUserCount;
        private long totalFileCount;
        private long totalFolderCount;
        private long totalUsedBytes;
        private long totalQuotaBytes;
        private double overallUsagePercent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanStatRes {
        private String planCode;
        private String planLabel;
        private long userCount;
        private double userPercent;
        private long usedBytes;
        private long quotaBytes;
        private double usagePercent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserRes {
        private Long idx;
        private String id;
        private String name;
        private String role;
        private String accountStatus;
        private boolean enabled;
        private String planCode;
        private String planLabel;
        private long usedBytes;
        private long quotaBytes;
        private double usagePercent;
        private long fileCount;
        private long folderCount;
        private long sharedFileCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateReq {
        private String accountStatus;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageAnalyticsRes {
        private StorageAnalyticsWindowRes window;
        private StorageSummaryRes summary;
        private StorageIntegrityRes integrity;
        private List<StorageBreakdownRes> storageBreakdown;
        private List<TransferBreakdownRes> transferBreakdown;
        private List<UserTransferStatRes> users;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageSummaryRes {
        private long providerCapacityBytes;
        private long providerUsedBytes;
        private long providerRemainingBytes;
        private double providerUsagePercent;
        private long allocatedUserQuotaBytes;
        private long allocatedUserUsedBytes;
        private double allocatedUserUsagePercent;
        private long totalIngressBytes;
        private long completedIngressBytes;
        private long canceledIngressBytes;
        private long totalEgressBytes;
        private long trackedUserCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageAnalyticsWindowRes {
        private String rangeCode;
        private String rangeLabel;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageIntegrityRes {
        private boolean healthy;
        private int issueCount;
        private List<String> issues;
        private long pendingDriveReservationBytes;
        private LocalDateTime generatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageBreakdownRes {
        private String source;
        private String label;
        private long storedBytes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferBreakdownRes {
        private String direction;
        private String source;
        private String label;
        private String status;
        private long bytes;
        private long eventCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserTransferStatRes {
        private Long idx;
        private String id;
        private String name;
        private String planCode;
        private String planLabel;
        private long quotaBytes;
        private long currentStoredBytes;
        private long totalIngressBytes;
        private long completedIngressBytes;
        private long canceledIngressBytes;
        private long totalEgressBytes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageCapacityUpdateReq {
        private Long providerCapacityBytes;
    }
}
