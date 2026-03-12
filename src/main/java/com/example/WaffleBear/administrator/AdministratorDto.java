package com.example.WaffleBear.administrator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
