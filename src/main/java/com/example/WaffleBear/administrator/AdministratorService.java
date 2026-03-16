package com.example.WaffleBear.administrator;

import com.example.WaffleBear.administrator.model.AdministratorDto;
import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.file.service.StoragePlanService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserAccountStatus;
import com.example.WaffleBear.user.repository.RefreshTokenRepository;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdministratorService {

    private static final String ADMIN_LOGIN_ID = "administrator@administrator.adm";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StoragePlanService storagePlanService;

    public AdministratorDto.DashboardRes getDashboard(AuthUserDetails adminUser) {
        validateAdministrator(adminUser);

        List<User> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getIdx, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<FileInfo> files = fileUpDownloadRepository.findAll();
        Map<Long, UserFileStat> statsByUserIdx = aggregateUserFileStats(files);

        long totalUsers = users.size();
        long activeUsers = users.stream().filter(this::isActiveAccount).count();
        long suspendedUsers = users.stream().filter(user -> resolveAccountStatus(user) == UserAccountStatus.SUSPENDED).count();
        long bannedUsers = users.stream().filter(user -> resolveAccountStatus(user) == UserAccountStatus.BANNED).count();

        long totalFileCount = statsByUserIdx.values().stream().mapToLong(UserFileStat::fileCount).sum();
        long totalFolderCount = statsByUserIdx.values().stream().mapToLong(UserFileStat::folderCount).sum();
        long totalUsedBytes = statsByUserIdx.values().stream().mapToLong(UserFileStat::usedBytes).sum();
        long totalQuotaBytes = users.stream().mapToLong(user -> resolvePlan(user).quotaBytes()).sum();

        List<AdministratorDto.UserRes> userResponses = users.stream()
                .map(user -> toUserResponse(user, statsByUserIdx.getOrDefault(user.getIdx(), UserFileStat.empty())))
                .toList();

        List<AdministratorDto.PlanStatRes> planStats = buildPlanStats(users, statsByUserIdx, totalUsers);

        return AdministratorDto.DashboardRes.builder()
                .summary(AdministratorDto.SummaryRes.builder()
                        .totalUserCount(totalUsers)
                        .activeUserCount(activeUsers)
                        .suspendedUserCount(suspendedUsers)
                        .bannedUserCount(bannedUsers)
                        .totalFileCount(totalFileCount)
                        .totalFolderCount(totalFolderCount)
                        .totalUsedBytes(totalUsedBytes)
                        .totalQuotaBytes(totalQuotaBytes)
                        .overallUsagePercent(toPercent(totalUsedBytes, totalQuotaBytes))
                        .build())
                .planStats(planStats)
                .users(userResponses)
                .build();
    }

    public AdministratorDto.UserRes updateUserStatus(
            AuthUserDetails adminUser,
            Long userIdx,
            AdministratorDto.StatusUpdateReq request
    ) {
        validateAdministrator(adminUser);

        if (userIdx == null || request == null || request.getAccountStatus() == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        User targetUser = userRepository.findById(userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        if (isProtectedAdministrator(targetUser)) {
            throw new AccessDeniedException("administrator account cannot be modified");
        }

        UserAccountStatus targetStatus = parseAccountStatus(request.getAccountStatus());
        targetUser.setAccountStatus(targetStatus);
        userRepository.save(targetUser);

        if (targetStatus != UserAccountStatus.ACTIVE) {
          refreshTokenRepository.deleteByEmail(targetUser.getEmail());
        }

        UserFileStat stat = aggregateUserFileStats(fileUpDownloadRepository.findAllByUser_Idx(targetUser.getIdx()))
                .getOrDefault(targetUser.getIdx(), UserFileStat.empty());
        return toUserResponse(targetUser, stat);
    }

    private void validateAdministrator(AuthUserDetails adminUser) {
        if (adminUser == null) {
            throw new AccessDeniedException("administrator only");
        }

        String userId = adminUser.getId();
        String role = adminUser.getRole();
        if (!ADMIN_ROLE.equals(role) || !ADMIN_LOGIN_ID.equals(userId)) {
            throw new AccessDeniedException("administrator only");
        }
    }

    private boolean isProtectedAdministrator(User user) {
        return user != null
                && ADMIN_ROLE.equals(user.getRole())
                && ADMIN_LOGIN_ID.equals(resolveUserId(user));
    }

    private UserAccountStatus parseAccountStatus(String value) {
        try {
            return UserAccountStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private List<AdministratorDto.PlanStatRes> buildPlanStats(
            List<User> users,
            Map<Long, UserFileStat> statsByUserIdx,
            long totalUsers
    ) {
        Map<String, PlanAggregate> aggregates = new HashMap<>();

        for (User user : users) {
            StoragePlan plan = resolvePlan(user);
            UserFileStat stat = statsByUserIdx.getOrDefault(user.getIdx(), UserFileStat.empty());
            aggregates.computeIfAbsent(plan.code(), ignored -> new PlanAggregate(plan))
                    .add(stat.usedBytes());
        }

        return aggregates.values().stream()
                .map(aggregate -> aggregate.toResponse(totalUsers))
                .sorted(Comparator.comparing(AdministratorDto.PlanStatRes::getQuotaBytes).reversed())
                .toList();
    }

    private Map<Long, UserFileStat> aggregateUserFileStats(List<FileInfo> files) {
        Map<Long, UserFileStat> statsByUserIdx = new HashMap<>();

        for (FileInfo file : files) {
            if (file.getUser() == null || file.getUser().getIdx() == null) {
                continue;
            }

            UserFileStat stat = statsByUserIdx.computeIfAbsent(file.getUser().getIdx(), ignored -> UserFileStat.empty());
            FileNodeType nodeType = resolveNodeType(file);
            long sizeBytes = nodeType == FileNodeType.FILE ? Math.max(0L, file.getFileSize() == null ? 0L : file.getFileSize()) : 0L;
            boolean shared = file.isSharedFile();
            statsByUserIdx.put(file.getUser().getIdx(), stat.add(nodeType, sizeBytes, shared));
        }

        return statsByUserIdx;
    }

    private AdministratorDto.UserRes toUserResponse(User user, UserFileStat stat) {
        StoragePlan plan = resolvePlan(user);
        return AdministratorDto.UserRes.builder()
                .idx(user.getIdx())
                .id(resolveUserId(user))
                .name(user.getName())
                .role(user.getRole())
                .accountStatus(resolveAccountStatus(user).name())
                .enabled(Boolean.TRUE.equals(user.getEnable()))
                .planCode(plan.code())
                .planLabel(plan.label())
                .usedBytes(stat.usedBytes())
                .quotaBytes(plan.quotaBytes())
                .usagePercent(toPercent(stat.usedBytes(), plan.quotaBytes()))
                .fileCount(stat.fileCount())
                .folderCount(stat.folderCount())
                .sharedFileCount(stat.sharedFileCount())
                .build();
    }

    private FileNodeType resolveNodeType(FileInfo file) {
        return file.getNodeType() == null ? FileNodeType.FILE : file.getNodeType();
    }

    private String resolveUserId(User user) {
        if (user == null) {
            return "";
        }

        return user.getEmail();
    }

    private UserAccountStatus resolveAccountStatus(User user) {
        return user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
    }

    private boolean isActiveAccount(User user) {
        return resolveAccountStatus(user) == UserAccountStatus.ACTIVE;
    }

    private double toPercent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }

        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private StoragePlan resolvePlan(User user) {
        StoragePlanService.StorageQuota quota = storagePlanService.resolveQuota(user);
        return new StoragePlan(quota.planCode(), quota.planLabel(), quota.totalQuotaBytes());
    }

    private record StoragePlan(String code, String label, long quotaBytes) {
    }

    private record UserFileStat(long fileCount, long folderCount, long usedBytes, long sharedFileCount) {
        private static UserFileStat empty() {
            return new UserFileStat(0L, 0L, 0L, 0L);
        }

        private UserFileStat add(FileNodeType nodeType, long sizeBytes, boolean shared) {
            if (nodeType == FileNodeType.FOLDER) {
                return new UserFileStat(fileCount, folderCount + 1, usedBytes, sharedFileCount);
            }
            return new UserFileStat(fileCount + 1, folderCount, usedBytes + sizeBytes, shared ? sharedFileCount + 1 : sharedFileCount);
        }
    }

    private static class PlanAggregate {
        private final StoragePlan plan;
        private long userCount;
        private long usedBytes;

        private PlanAggregate(StoragePlan plan) {
            this.plan = plan;
        }

        private void add(long userUsedBytes) {
            this.userCount += 1;
            this.usedBytes += userUsedBytes;
        }

        private AdministratorDto.PlanStatRes toResponse(long totalUsers) {
            long totalQuotaBytes = userCount * plan.quotaBytes();
            return AdministratorDto.PlanStatRes.builder()
                    .planCode(plan.code())
                    .planLabel(plan.label())
                    .userCount(userCount)
                    .userPercent(totalUsers <= 0 ? 0.0 : Math.round((userCount * 10000.0) / totalUsers) / 100.0)
                    .usedBytes(usedBytes)
                    .quotaBytes(totalQuotaBytes)
                    .usagePercent(totalQuotaBytes <= 0 ? 0.0 : Math.round((usedBytes * 10000.0) / totalQuotaBytes) / 100.0)
                    .build();
        }
    }
}