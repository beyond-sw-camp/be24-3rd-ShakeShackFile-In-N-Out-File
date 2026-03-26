package com.example.WaffleBear.administrator;

import com.example.WaffleBear.administrator.model.AdministratorDto;
import com.example.WaffleBear.administrator.storage.DataTransferDirection;
import com.example.WaffleBear.administrator.storage.DataTransferLedger;
import com.example.WaffleBear.administrator.storage.DataTransferLedgerRepository;
import com.example.WaffleBear.administrator.storage.DataTransferSource;
import com.example.WaffleBear.administrator.storage.DataTransferStatus;
import com.example.WaffleBear.administrator.storage.StorageAnalyticsConfig;
import com.example.WaffleBear.administrator.storage.StorageAnalyticsConfigRepository;
import com.example.WaffleBear.chat.ChatMessageRepository;
import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.file.service.StoragePlanService;
import com.example.WaffleBear.file.upload.UploadService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.workspace.asset.WorkspaceAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StorageAnalyticsService {

    private static final Long CONFIG_ID = 1L;

    private final UserRepository userRepository;
    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final WorkspaceAssetRepository workspaceAssetRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final StoragePlanService storagePlanService;
    private final StorageAnalyticsConfigRepository storageAnalyticsConfigRepository;
    private final DataTransferLedgerRepository dataTransferLedgerRepository;
    private final ObjectProvider<UploadService> uploadServiceProvider;

    public AdministratorDto.StorageAnalyticsRes getStorageAnalytics(AuthUserDetails adminUser, String rangeCode) {
        validateAdministrator(adminUser);

        UploadService uploadService = uploadServiceProvider.getIfAvailable();
        if (uploadService != null) {
            uploadService.synchronizeExpiredReservations();
        }

        AnalyticsWindow window = AnalyticsWindow.resolve(rangeCode);
        List<User> users = userRepository.findAll();
        List<User> trackedUsers = users.stream()
                .filter(user -> !storagePlanService.isAdministrator(user))
                .toList();

        Map<Long, Long> driveStoredBytesByUser = new HashMap<>();
        Map<Long, Long> workspaceStoredBytesByUser = new HashMap<>();
        Map<Long, Long> chatStoredBytesByUser = new HashMap<>();

        long driveStoredBytes = accumulateDriveStorage(driveStoredBytesByUser);
        long workspaceStoredBytes = accumulateWorkspaceStorage(workspaceStoredBytesByUser);
        long chatStoredBytes = accumulateChatStorage(chatStoredBytesByUser);
        long providerUsedBytes = driveStoredBytes + workspaceStoredBytes + chatStoredBytes;

        long allocatedUserQuotaBytes = trackedUsers.stream()
                .map(storagePlanService::resolveQuota)
                .mapToLong(StoragePlanService.StorageQuota::totalQuotaBytes)
                .sum();
        long allocatedUserUsedBytes = trackedUsers.stream()
                .mapToLong(user -> resolveUserStoredBytes(
                        user.getIdx(),
                        driveStoredBytesByUser,
                        workspaceStoredBytesByUser,
                        chatStoredBytesByUser
                ))
                .sum();

        StorageAnalyticsConfig config = findOrCreateConfig();
        long providerCapacityBytes = Math.max(
                0L,
                Objects.requireNonNullElse(
                        config.getProviderCapacityBytes(),
                        StorageAnalyticsConfig.DEFAULT_PROVIDER_CAPACITY_BYTES
                )
        );
        long providerRemainingBytes = Math.max(0L, providerCapacityBytes - providerUsedBytes);

        List<DataTransferLedger> ledgers = loadTransferLedgers(window.startedAt());
        TransferAccumulator transferAccumulator = new TransferAccumulator();
        ledgers.forEach(transferAccumulator::add);

        long pendingDriveReservationBytes = uploadService == null ? 0L : uploadService.getPendingReservedBytes();
        List<String> integrityIssues = buildIntegrityIssues(
                providerCapacityBytes,
                providerUsedBytes,
                allocatedUserQuotaBytes,
                allocatedUserUsedBytes,
                pendingDriveReservationBytes
        );

        return AdministratorDto.StorageAnalyticsRes.builder()
                .window(AdministratorDto.StorageAnalyticsWindowRes.builder()
                        .rangeCode(window.code())
                        .rangeLabel(window.label())
                        .startedAt(window.startedAt())
                        .endedAt(window.endedAt())
                        .build())
                .summary(AdministratorDto.StorageSummaryRes.builder()
                        .providerCapacityBytes(providerCapacityBytes)
                        .providerUsedBytes(providerUsedBytes)
                        .providerRemainingBytes(providerRemainingBytes)
                        .providerUsagePercent(toPercent(providerUsedBytes, providerCapacityBytes))
                        .allocatedUserQuotaBytes(allocatedUserQuotaBytes)
                        .allocatedUserUsedBytes(allocatedUserUsedBytes)
                        .allocatedUserUsagePercent(toPercent(allocatedUserUsedBytes, allocatedUserQuotaBytes))
                        .totalIngressBytes(transferAccumulator.totalIngressBytes)
                        .completedIngressBytes(transferAccumulator.completedIngressBytes)
                        .canceledIngressBytes(transferAccumulator.canceledIngressBytes)
                        .totalEgressBytes(transferAccumulator.totalEgressBytes)
                        .trackedUserCount(trackedUsers.size())
                        .build())
                .integrity(AdministratorDto.StorageIntegrityRes.builder()
                        .healthy(integrityIssues.isEmpty())
                        .issueCount(integrityIssues.size())
                        .issues(integrityIssues)
                        .pendingDriveReservationBytes(pendingDriveReservationBytes)
                        .generatedAt(LocalDateTime.now())
                        .build())
                .storageBreakdown(List.of(
                        AdministratorDto.StorageBreakdownRes.builder()
                                .source("DRIVE")
                                .label("드라이브")
                                .storedBytes(driveStoredBytes)
                                .build(),
                        AdministratorDto.StorageBreakdownRes.builder()
                                .source("WORKSPACE")
                                .label("워크스페이스")
                                .storedBytes(workspaceStoredBytes)
                                .build(),
                        AdministratorDto.StorageBreakdownRes.builder()
                                .source("CHAT")
                                .label("채팅")
                                .storedBytes(chatStoredBytes)
                                .build()
                ))
                .transferBreakdown(transferAccumulator.toResponses())
                .users(buildUserTransferStats(
                        trackedUsers,
                        driveStoredBytesByUser,
                        workspaceStoredBytesByUser,
                        chatStoredBytesByUser,
                        transferAccumulator.byUser
                ))
                .build();
    }

    public AdministratorDto.StorageAnalyticsRes updateProviderCapacity(
            AuthUserDetails adminUser,
            AdministratorDto.StorageCapacityUpdateReq request,
            String rangeCode
    ) {
        validateAdministrator(adminUser);

        long providerCapacityBytes = request == null || request.getProviderCapacityBytes() == null
                ? -1L
                : request.getProviderCapacityBytes();
        if (providerCapacityBytes <= 0L) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        StorageAnalyticsConfig config = findOrCreateConfig();
        config.changeProviderCapacityBytes(providerCapacityBytes);
        storageAnalyticsConfigRepository.save(config);
        return getStorageAnalytics(adminUser, rangeCode);
    }

    public void recordIngress(
            Long userIdx,
            DataTransferSource source,
            DataTransferStatus status,
            long bytes,
            String objectKey,
            String referenceLabel
    ) {
        saveLedger(userIdx, DataTransferDirection.INGRESS, source, status, bytes, objectKey, referenceLabel);
    }

    public void recordEgress(
            Long userIdx,
            DataTransferSource source,
            DataTransferStatus status,
            long bytes,
            String objectKey,
            String referenceLabel
    ) {
        saveLedger(userIdx, DataTransferDirection.EGRESS, source, status, bytes, objectKey, referenceLabel);
    }

    private void saveLedger(
            Long userIdx,
            DataTransferDirection direction,
            DataTransferSource source,
            DataTransferStatus status,
            long bytes,
            String objectKey,
            String referenceLabel
    ) {
        if (direction == null || source == null || status == null || bytes <= 0L) {
            return;
        }

        dataTransferLedgerRepository.save(
                DataTransferLedger.builder()
                        .user(userIdx == null || userIdx <= 0L ? null : User.builder().idx(userIdx).build())
                        .direction(direction)
                        .source(source)
                        .status(status)
                        .bytes(bytes)
                        .objectKey(objectKey)
                        .referenceLabel(referenceLabel)
                        .build()
        );
    }

    private StorageAnalyticsConfig findOrCreateConfig() {
        return storageAnalyticsConfigRepository.findById(CONFIG_ID)
                .orElseGet(() -> storageAnalyticsConfigRepository.save(
                        StorageAnalyticsConfig.builder()
                                .id(CONFIG_ID)
                                .providerCapacityBytes(StorageAnalyticsConfig.DEFAULT_PROVIDER_CAPACITY_BYTES)
                                .build()
                ));
    }

    private long accumulateDriveStorage(Map<Long, Long> storedBytesByUser) {
        long total = 0L;
        for (FileUpDownloadRepository.UserDashboardStatProjection row :
                fileUpDownloadRepository.aggregateDashboardStatsByUser(FileNodeType.FOLDER)) {
            Long userIdx = row.getUserIdx();
            long storedBytes = Math.max(0L, row.getUsedBytes() == null ? 0L : row.getUsedBytes());
            if (userIdx != null) {
                storedBytesByUser.put(userIdx, storedBytes);
            }
            total += storedBytes;
        }
        return total;
    }

    private long accumulateWorkspaceStorage(Map<Long, Long> storedBytesByUser) {
        long total = 0L;
        for (WorkspaceAssetRepository.UserStoredBytesProjection row : workspaceAssetRepository.aggregateStoredBytesByUploader()) {
            Long userIdx = row.getUserIdx();
            long storedBytes = Math.max(0L, row.getStoredBytes() == null ? 0L : row.getStoredBytes());
            if (userIdx != null) {
                storedBytesByUser.put(userIdx, storedBytes);
            }
            total += storedBytes;
        }
        return total;
    }

    private long accumulateChatStorage(Map<Long, Long> storedBytesByUser) {
        long total = 0L;
        for (ChatMessageRepository.UserStoredBytesProjection row : chatMessageRepository.aggregateStoredBytesBySender()) {
            Long userIdx = row.getUserIdx();
            long storedBytes = Math.max(0L, row.getStoredBytes() == null ? 0L : row.getStoredBytes());
            if (userIdx != null) {
                storedBytesByUser.put(userIdx, storedBytes);
            }
            total += storedBytes;
        }
        return total;
    }

    private List<AdministratorDto.UserTransferStatRes> buildUserTransferStats(
            List<User> users,
            Map<Long, Long> driveStoredBytesByUser,
            Map<Long, Long> workspaceStoredBytesByUser,
            Map<Long, Long> chatStoredBytesByUser,
            Map<Long, UserTransferAccumulator> transferByUser
    ) {
        return users.stream()
                .map(user -> {
                    StoragePlanService.StorageQuota quota = storagePlanService.resolveQuota(user);
                    UserTransferAccumulator transfer = transferByUser.getOrDefault(user.getIdx(), UserTransferAccumulator.empty());
                    return AdministratorDto.UserTransferStatRes.builder()
                            .idx(user.getIdx())
                            .id(user.getEmail())
                            .name(user.getName())
                            .planCode(quota.planCode())
                            .planLabel(quota.planLabel())
                            .quotaBytes(quota.totalQuotaBytes())
                            .currentStoredBytes(resolveUserStoredBytes(
                                    user.getIdx(),
                                    driveStoredBytesByUser,
                                    workspaceStoredBytesByUser,
                                    chatStoredBytesByUser
                            ))
                            .totalIngressBytes(transfer.totalIngressBytes)
                            .completedIngressBytes(transfer.completedIngressBytes)
                            .canceledIngressBytes(transfer.canceledIngressBytes)
                            .totalEgressBytes(transfer.totalEgressBytes)
                            .build();
                })
                .sorted(Comparator.comparingLong(AdministratorDto.UserTransferStatRes::getCurrentStoredBytes).reversed())
                .toList();
    }

    private long resolveUserStoredBytes(
            Long userIdx,
            Map<Long, Long> driveStoredBytesByUser,
            Map<Long, Long> workspaceStoredBytesByUser,
            Map<Long, Long> chatStoredBytesByUser
    ) {
        if (userIdx == null) {
            return 0L;
        }

        return driveStoredBytesByUser.getOrDefault(userIdx, 0L)
                + workspaceStoredBytesByUser.getOrDefault(userIdx, 0L)
                + chatStoredBytesByUser.getOrDefault(userIdx, 0L);
    }

    private List<String> buildIntegrityIssues(
            long providerCapacityBytes,
            long providerUsedBytes,
            long allocatedUserQuotaBytes,
            long allocatedUserUsedBytes,
            long pendingDriveReservationBytes
    ) {
        List<String> issues = new ArrayList<>();

        if (providerCapacityBytes <= 0L) {
            issues.add("스토리지 공급 총용량이 0 이하로 설정되어 있습니다.");
        }
        if (providerCapacityBytes > 0L && providerUsedBytes > providerCapacityBytes) {
            issues.add("현재 저장된 총 데이터가 스토리지 공급 총용량을 초과했습니다.");
        }
        if (allocatedUserQuotaBytes > 0L && allocatedUserUsedBytes > allocatedUserQuotaBytes) {
            issues.add("일반 사용자 할당 총용량보다 실제 사용량이 더 많습니다.");
        }
        if (pendingDriveReservationBytes > 0L) {
            issues.add("정리되지 않은 드라이브 업로드 예약 용량이 남아 있습니다.");
        }

        return issues;
    }

    private void validateAdministrator(AuthUserDetails adminUser) {
        if (adminUser == null) {
            throw new AccessDeniedException("administrator only");
        }

        String role = adminUser.getRole();
        if (role != null && role.toUpperCase(Locale.ROOT).contains("ADMIN")) {
            return;
        }

        String email = adminUser.getEmail();
        if (email == null || email.isBlank()) {
            email = adminUser.getId();
        }

        if (!storagePlanService.isAdministrator(
                User.builder()
                        .email(email)
                        .role(role)
                        .build()
        )) {
            throw new AccessDeniedException("administrator only");
        }
    }

    private List<DataTransferLedger> loadTransferLedgers(LocalDateTime startedAt) {
        if (startedAt == null) {
            return dataTransferLedgerRepository.findAllByOrderByCreatedAtDesc();
        }
        return dataTransferLedgerRepository.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(startedAt);
    }

    private double toPercent(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0.0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private record AnalyticsWindow(
            String code,
            String label,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        private static AnalyticsWindow resolve(String rawCode) {
            LocalDateTime now = LocalDateTime.now();
            String normalized = rawCode == null || rawCode.isBlank()
                    ? "24H"
                    : rawCode.trim().toUpperCase(Locale.ROOT);

            return switch (normalized) {
                case "1H" -> new AnalyticsWindow("1H", "최근 1시간", now.minusHours(1), now);
                case "12H" -> new AnalyticsWindow("12H", "최근 12시간", now.minusHours(12), now);
                case "24H" -> new AnalyticsWindow("24H", "최근 24시간", now.minusHours(24), now);
                case "1D" -> new AnalyticsWindow("1D", "오늘", LocalDate.now().atStartOfDay(), now);
                case "3D" -> new AnalyticsWindow("3D", "최근 3일", now.minusDays(3), now);
                case "7D" -> new AnalyticsWindow("7D", "최근 7일", now.minusDays(7), now);
                case "4W" -> new AnalyticsWindow("4W", "최근 4주", now.minusWeeks(4), now);
                default -> new AnalyticsWindow("24H", "최근 24시간", now.minusHours(24), now);
            };
        }
    }

    private static class TransferAccumulator {
        private final Map<TransferKey, long[]> grouped = new HashMap<>();
        private final Map<Long, UserTransferAccumulator> byUser = new HashMap<>();
        private long totalIngressBytes;
        private long completedIngressBytes;
        private long canceledIngressBytes;
        private long totalEgressBytes;

        private void add(DataTransferLedger ledger) {
            if (ledger == null || ledger.getDirection() == null || ledger.getSource() == null || ledger.getStatus() == null) {
                return;
            }

            long bytes = Math.max(0L, ledger.getBytes() == null ? 0L : ledger.getBytes());
            if (bytes <= 0L) {
                return;
            }

            TransferKey key = new TransferKey(ledger.getDirection(), ledger.getSource(), ledger.getStatus());
            grouped.computeIfAbsent(key, ignored -> new long[2]);
            long[] aggregate = grouped.get(key);
            aggregate[0] += bytes;
            aggregate[1] += 1L;

            Long userIdx = ledger.getUser() != null ? ledger.getUser().getIdx() : null;
            if (userIdx != null) {
                byUser.computeIfAbsent(userIdx, ignored -> UserTransferAccumulator.empty())
                        .add(ledger.getDirection(), ledger.getStatus(), bytes);
            }

            if (ledger.getDirection() == DataTransferDirection.INGRESS) {
                totalIngressBytes += bytes;
                if (ledger.getStatus() == DataTransferStatus.COMPLETED) {
                    completedIngressBytes += bytes;
                }
                if (ledger.getStatus() == DataTransferStatus.ABORTED) {
                    canceledIngressBytes += bytes;
                }
            } else {
                totalEgressBytes += bytes;
            }
        }

        private List<AdministratorDto.TransferBreakdownRes> toResponses() {
            return grouped.entrySet().stream()
                    .map(entry -> AdministratorDto.TransferBreakdownRes.builder()
                            .direction(entry.getKey().direction.name())
                            .source(entry.getKey().source.name())
                            .label(entry.getKey().source.getLabel())
                            .status(entry.getKey().status.name())
                            .bytes(entry.getValue()[0])
                            .eventCount(entry.getValue()[1])
                            .build())
                    .sorted(Comparator.comparingLong(AdministratorDto.TransferBreakdownRes::getBytes).reversed())
                    .toList();
        }
    }

    private record TransferKey(
            DataTransferDirection direction,
            DataTransferSource source,
            DataTransferStatus status
    ) {
    }

    private static class UserTransferAccumulator {
        private long totalIngressBytes;
        private long completedIngressBytes;
        private long canceledIngressBytes;
        private long totalEgressBytes;

        private static UserTransferAccumulator empty() {
            return new UserTransferAccumulator();
        }

        private UserTransferAccumulator add(DataTransferDirection direction, DataTransferStatus status, long bytes) {
            if (direction == DataTransferDirection.INGRESS) {
                totalIngressBytes += bytes;
                if (status == DataTransferStatus.COMPLETED) {
                    completedIngressBytes += bytes;
                }
                if (status == DataTransferStatus.ABORTED) {
                    canceledIngressBytes += bytes;
                }
            } else {
                totalEgressBytes += bytes;
            }
            return this;
        }
    }
}
