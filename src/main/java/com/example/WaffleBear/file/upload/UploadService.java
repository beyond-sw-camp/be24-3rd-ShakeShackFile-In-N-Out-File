package com.example.WaffleBear.file.upload;

import com.example.WaffleBear.administrator.StorageAnalyticsService;
import com.example.WaffleBear.administrator.storage.DataTransferSource;
import com.example.WaffleBear.administrator.storage.DataTransferStatus;
import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.file.service.StoragePlanService;
import com.example.WaffleBear.file.upload.dto.UploadDto;
import com.example.WaffleBear.user.model.User;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final long PARTITION_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long CHUNK_SIZE_BYTES = 80L * 1024 * 1024;
    private static final long MIN_FINAL_PARTITION_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long RESERVATION_GRACE_SECONDS = 5L * 60L;
    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final StoragePlanService storagePlanService;
    private final UploadFolderService uploadFolderService;
    private final StorageAnalyticsService storageAnalyticsService;
    private final Object uploadReservationMonitor = new Object();
    private final Map<String, UploadReservation> uploadReservations = new ConcurrentHashMap<>();

    // 해당 메서드 내에서 DB작업이 날 경우 전부 성공할 경우만 반영, 중간에 하나라도 실패시 롤백
    @Transactional
    // 맨처음 파일 저장하기 위한 작업 init이다 유저 번호, 업로드할 파일 정보들을 가지고
    public List<UploadDto.ChunkRes> init(Long userIdx, List<UploadDto.InitReq> requests) {
        // 유저 번호가 0이면 (컨트롤러에서 초기화 시켰음) 에러코드 날리고 종료
        validateUser(userIdx);

        // 만약 요청한 파일이 없거나, null일경우 에러코드 날리고 종료
        if (requests == null || requests.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        // 이 사용자의 저장소 요금제/플랜 정보를 조회 최대 업로드 가능 개수, 최대 파일 크기, 총 저장 용량 제한 등 확인을 위해 객채 생성
        StoragePlanService.StorageQuota storageQuota = storagePlanService.resolveQuota(userIdx);

        // 업로드하려고 하는 파일의 개수를 카운트해서, 사용자 플랜 정보에서 값을 가져와서 비교
        if (requests.size() > storageQuota.maxUploadCount()) {
            // 현재 플랜의 허용하는 파일 갯수가 초과한 경우 종료
            throw BaseException.from(BaseResponseStatus.FILE_COUNT_WRONG);
        }

        // 모든 파일 크기, 이름, 사이즈, 널인지, 이름에 이상있는지 등을 확인
        long requiredBytes = 0L;
        for (UploadDto.InitReq request : requests) {
            //
            validate(request, storageQuota.maxUploadFileBytes());
            requiredBytes += Math.max(0L, request.getFileSize() == null ? 0L : request.getFileSize());
        }

        // 업로드할 파일을 위한 리스트 생성
        List<UploadDto.ChunkRes> responses = new ArrayList<>();
        // 업로드 예정 파일들의 "용량 예약 목록"을 저장할 리스트
        List<PendingUploadReservation> pendingReservations = new ArrayList<>();
        // 클라이언트가 보낸 파일 업로드 요청들을 하나씩 처리
        for (UploadDto.InitReq request : requests) {
            // 파일 업로드를 시작하기 위한 초기 응답을 생성, presigned URL을 생성
            List<UploadDto.ChunkRes> initResponses = buildInitResponses(userIdx, request);
            // 지금까지 문제가 없었으므로 해당 객체를 리스트에 저장
            responses.addAll(initResponses);

            // 업로드 초기화 응답이 정상적으로 생성된 경우에만 아래 코드를 실행
            if (!initResponses.isEmpty()) {
                // pendingReservations는 업로드 예정 파일들의 "용량 예약 목록"
                // PendingUploadReservation 객체를 새로 만들어서 pendingReservations 리스트에 추가
                pendingReservations.add(new PendingUploadReservation(
                        // initResponses 리스트의 첫 번째 요소를 가져온다, getFinalObjectKey() → 최종 저장될 파일의 objectKey
                        initResponses.get(0).getFinalObjectKey(),
                        // 파일 크기를 안전하게 계산하는 코드, request.getFileSize() 가 null이면 → 0 사용 fileSize가 음수일 경우 → 최소 0으로 보정
                        Math.max(0L, request.getFileSize() == null ? 0L : request.getFileSize())

                        // 이렇게 PendingUploadReservation 객체 생성 완료 그리고 pendingReservations 리스트에 추가
                ));
            }
        }

        // 동기화, uploadReservationMonitor 객체를 기준으로 동기화(lock) 걸기, 이유: 저장 용량(quota) 계산이 동시에 실행되면 데이터가 꼬일 수 있기 때문
        synchronized (uploadReservationMonitor) {
            // 시간이 지나 만료된 업로드 용량 예약을 자동으로 삭제하기
            cleanupExpiredUploadReservations();
            //
            ensureWithinStorageQuota(userIdx, requiredBytes, null);
            pendingReservations.forEach(reservation -> reserveUploadQuota(userIdx, reservation.finalObjectKey(), reservation.fileSize()));
        }

        return responses;
    }

    @Transactional
    public UploadDto.CompleteRes complete(Long userIdx, UploadDto.CompleteReq request) {
        validateUser(userIdx);
        validateCompleteRequest(request);

        String fileOriginName = normalizeOriginName(request.getFileOriginName());
        String fileFormat = normalizeFormat(request.getFileFormat(), fileOriginName);
        String finalObjectKey = normalizeOwnedObjectKey(userIdx, request.getFinalObjectKey());
        List<String> chunkObjectKeys = normalizeOwnedObjectKeys(userIdx, request.getChunkObjectKeys());
        long expectedFileSize = Math.max(0L, request.getFileSize() == null ? 0L : request.getFileSize());

        cleanupExpiredUploadReservations();

        Optional<FileInfo> existing = fileUpDownloadRepository.findByUser_IdxAndFileSavePath(userIdx, finalObjectKey);
        if (existing.isPresent()) {
            releaseUploadQuota(finalObjectKey);
            FileInfo entity = existing.get();
            return UploadDto.CompleteRes.builder()
                    .fileOriginName(entity.getFileOriginName())
                    .fileSaveName(entity.getFileSaveName())
                    .fileFormat(entity.getFileFormat())
                    .finalObjectKey(finalObjectKey)
                    .build();
        }

        if (chunkObjectKeys.isEmpty()) {
            ensureUploadedObjectExists(finalObjectKey);
        } else {
            ensureAllUploaded(chunkObjectKeys);
            if (!objectExists(finalObjectKey)) {
                composeFinalObject(finalObjectKey, chunkObjectKeys);
            }
            ensureUploadedObjectExists(finalObjectKey);
        }

        long actualFileSize = resolveCompletedObjectSize(finalObjectKey, expectedFileSize);
        ensureWithinStorageQuota(userIdx, actualFileSize, finalObjectKey);

        saveFinalFileInfo(userIdx, request, fileOriginName, fileFormat, actualFileSize, finalObjectKey);
        releaseUploadQuota(finalObjectKey);
        deleteObjectKeys(chunkObjectKeys);
        storageAnalyticsService.recordIngress(
                userIdx,
                DataTransferSource.DRIVE_UPLOAD,
                DataTransferStatus.COMPLETED,
                actualFileSize,
                finalObjectKey,
                fileOriginName
        );

        return UploadDto.CompleteRes.builder()
                .fileOriginName(fileOriginName)
                .fileSaveName(extractFileSaveName(finalObjectKey))
                .fileFormat(fileFormat)
                .finalObjectKey(finalObjectKey)
                .build();
    }

    @Transactional
    public UploadDto.ActionRes abort(Long userIdx, UploadDto.AbortReq request) {
        validateUser(userIdx);
        cleanupExpiredUploadReservations();

        List<String> cleanupTargets = normalizeAbortTargets(userIdx, request);
        recordAbortedUploadBytes(userIdx, cleanupTargets, request != null ? request.getFinalObjectKey() : null);
        deleteObjectKeys(cleanupTargets);
        releaseUploadQuota(request != null ? request.getFinalObjectKey() : null);

        return UploadDto.ActionRes.builder()
                .action("abort-upload")
                .affectedCount(cleanupTargets.size())
                .build();
    }

    public void synchronizeExpiredReservations() {
        cleanupExpiredUploadReservations();
    }

    public long getPendingReservedBytes() {
        cleanupExpiredUploadReservations();
        return uploadReservations.values().stream()
                .mapToLong(UploadReservation::reservedBytes)
                .sum();
    }

    // 업로드를 요청한 사용자의 ID(userIdx)와, 프론트에서 보낸 업로드 초기 요청 정보(request)를 받아서, 업로드용 응답 목록(List<ChunkRes>)을 만드는 메서드
    private List<UploadDto.ChunkRes> buildInitResponses(Long userIdx, UploadDto.InitReq request) {
        // 파일 원본이름 가져오기
        String fileOriginName = normalizeOriginName(request.getFileOriginName());
        // 파일 포멧 가져오기
        String fileFormat = normalizeFormat(request.getFileFormat(), fileOriginName);
        // 파일 경로 정리해서 가져오기(공백제거)
        String relativePath = normalizeRelativePath(request.getRelativePath(), fileOriginName);
        //
        Long parentId = normalizeParentId(request.getParentId());
        Long lastModified = normalizeLastModified(request.getLastModified());
        long fileSize = Math.max(0L, request.getFileSize() == null ? 0L : request.getFileSize());

        int partitionCount = calculatePartitionCount(fileSize);
        boolean partitioned = partitionCount > 1;
        String fileSaveName = buildFileSaveName(fileFormat);
        String finalObjectKey = userIdx + "/" + fileSaveName;
        String partitionBase = partitioned ? UUID.randomUUID().toString() : null;

        List<UploadDto.ChunkRes> responses = new ArrayList<>();
        for (int index = 0; index < partitionCount; index += 1) {
            String objectKey = partitioned
                    ? buildPartitionObjectKey(userIdx + "/", partitionBase, fileFormat, index, partitionCount)
                    : finalObjectKey;

            responses.add(UploadDto.ChunkRes.builder()
                    .fileOriginName(fileOriginName)
                    .fileSaveName(fileSaveName)
                    .fileFormat(fileFormat)
                    .fileSize(fileSize)
                    .contentType(normalizeContentType(request.getContentType()))
                    .parentId(parentId)
                    .relativePath(relativePath)
                    .lastModified(lastModified)
                    .presignedUploadUrl(generatePresignedUploadUrl(objectKey))
                    .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                    .objectKey(objectKey)
                    .finalObjectKey(finalObjectKey)
                    .partitionIndex(index + 1)
                    .partitionCount(partitionCount)
                    .partitioned(partitioned)
                    .uploaded(false)
                    .build());
        }
        return responses;
    }

    private void validateUser(Long userIdx) {
        if (userIdx == null || userIdx <= 0) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    // 각각 파일에 대해 정상적인지 검사
    // 하나라도 걸리면 업로드 종료
    private void validate(UploadDto.InitReq request, long maxUploadFileBytes) {
        // 파일이 없을 경우
        if (request == null) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        // 이름이 없을경우, 이름이 널일 경우
        String originName = request.getFileOriginName();
        if (originName == null || originName.isBlank()) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        // 이름이 100자 초과할 경우
        if (originName.length() > 100) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_LENGTH_WRONG);
        }

        // 이름에 ..이렇게 혹은 이름에 /슬래시 혹은 \\역슬래시 두개가 혹은 \u0000(즉 **“문자열의 끝 또는 비어있는 문자”**를 의미하는 특수 문자)가 있을 경우
        if (originName.contains("..") || originName.contains("/") || originName.contains("\\") || originName.contains("\u0000")) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        // 파일 사이즈가 업로드할 수 있는 최대값을 넘은 경우
        Long fileSize = request.getFileSize();
        if (fileSize != null && fileSize > maxUploadFileBytes) {
            throw BaseException.from(BaseResponseStatus.FILE_SIZE_WRONG);
        }
    }

    private void validateCompleteRequest(UploadDto.CompleteReq request) {
        if (request == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        if (request.getFileOriginName() == null || request.getFileOriginName().isBlank()) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        if (request.getFinalObjectKey() == null || request.getFinalObjectKey().isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    // 사용자의 저장 용량(quota)을 초과하지 않는지 확인하는 메서드
    // additionalBytes = 이번 업로드 요청으로 추가될 파일 용량
    // ignoredReservationKey = 계산에서 제외할 예약 key (특정 예약 무시할 때 사용)
    private void ensureWithinStorageQuota(Long userIdx, long additionalBytes, String ignoredReservationKey) {
        // 사용자의 플랜의 최대 업로드 가능 용량을 가져옴, storagePlanService → 사용자 요금제/플랜 정보
        // totalQuotaBytes() → 총 저장 가능 용량
        long quotaBytes = storagePlanService.resolveQuota(userIdx).totalQuotaBytes();
        long usedBytes = resolveUsedStorageBytes(userIdx);
        long reservedBytes = resolveReservedStorageBytes(userIdx, ignoredReservationKey);

        if (quotaBytes > 0 && usedBytes + reservedBytes + Math.max(0L, additionalBytes) > quotaBytes) {
            throw BaseException.from(BaseResponseStatus.STORAGE_QUOTA_EXCEEDED);
        }
    }

    private long resolveUsedStorageBytes(Long userIdx) {
        Long usedBytes = fileUpDownloadRepository.sumStoredFileBytesByUser(userIdx, FileNodeType.FILE);
        return Math.max(0L, usedBytes == null ? 0L : usedBytes);
    }

    private long resolveReservedStorageBytes(Long userIdx, String ignoredReservationKey) {
        cleanupExpiredUploadReservations();

        return uploadReservations.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getKey(), ignoredReservationKey))
                .map(Map.Entry::getValue)
                .filter(reservation -> Objects.equals(reservation.userIdx(), userIdx))
                .mapToLong(UploadReservation::reservedBytes)
                .sum();
    }

    private void reserveUploadQuota(Long userIdx, String finalObjectKey, long fileSize) {
        if (finalObjectKey == null || finalObjectKey.isBlank() || fileSize <= 0) {
            return;
        }

        uploadReservations.put(finalObjectKey, new UploadReservation(
                userIdx,
                fileSize,
                System.currentTimeMillis() + ((long) minioProperties.getPresignedUrlExpirySeconds() + RESERVATION_GRACE_SECONDS) * 1000L
        ));
    }

    private void releaseUploadQuota(String finalObjectKey) {
        if (finalObjectKey == null || finalObjectKey.isBlank()) {
            return;
        }

        uploadReservations.remove(finalObjectKey.trim());
    }

    // 업로드 예약 목록에서 "만료된 예약"을 삭제하는 메서드
    private void cleanupExpiredUploadReservations() {
        // 현재 시간을 밀리초(ms) 단위로 가져온다
        long now = System.currentTimeMillis();
        // 업로드 예약 정보를 저장하는 Map 참고로 final이어도 Map 안의 데이터는 변경할 수 있다.
        // final은 객체 내부 데이터가 아니라 참조(레퍼런스)를 고정한다.

        // 예약 목록을 검사해서 만료시간 <= 현재시간인 예약을 모두 삭제, 즉 시간이 지나 만료된 업로드 용량 예약을 자동으로 삭제하는 코드다.
        uploadReservations.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private int calculatePartitionCount(long fileSize) {
        if (fileSize <= PARTITION_SIZE_BYTES) {
            return 1;
        }

        long partitionCount = (fileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES;
        long remainder = fileSize % CHUNK_SIZE_BYTES;
        if (partitionCount > 1 && remainder > 0 && remainder <= MIN_FINAL_PARTITION_SIZE_BYTES) {
            return (int) (partitionCount - 1);
        }

        return (int) partitionCount;
    }

    private String buildFileSaveName(String fileFormat) {
        return UUID.randomUUID() + "." + fileFormat;
    }

    private String buildPartitionObjectKey(String basicPath, String partitionBase, String fileFormat, int partitionIndex, int partitionCount) {
        return basicPath
                + "tmp/"
                + partitionBase
                + ".part"
                + String.format("%05d", partitionIndex + 1)
                + "of"
                + String.format("%05d", partitionCount)
                + "."
                + fileFormat;
    }

    private String normalizeOriginName(String originName) {
        String normalized = originName == null ? "" : originName.trim();
        if (normalized.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }
        return normalized;
    }

    private String normalizeFormat(String rawFormat, String originName) {
        String format = rawFormat;
        if (format == null || format.isBlank()) {
            int idx = originName.lastIndexOf('.');
            if (idx <= 0 || idx >= originName.length() - 1) {
                throw BaseException.from(BaseResponseStatus.FILE_FORMAT_NOTHING);
            }
            format = originName.substring(idx + 1);
        }

        format = format.trim();
        if (format.startsWith(".")) {
            format = format.substring(1);
        }

        if (format.isEmpty() || format.length() > 20 || !format.matches("^[A-Za-z0-9]+$")) {
            throw BaseException.from(BaseResponseStatus.FILE_FORMAT_WRONG);
        }

        return format.toLowerCase();
    }

    private String normalizeContentType(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim();
        return normalized.isEmpty() ? "application/octet-stream" : normalized;
    }

    // 파일의 상대 경로를 정리(일반, 일관적을 정리하기)
    private String normalizeRelativePath(String relativePath, String fallbackFileName) {
        // 프론트에서 전달된 상대 경로가(relativePath)가 널이면, 빈문자열("")으로 변경
        String normalized = relativePath == null ? "" : relativePath.trim();
        if (normalized.isEmpty()) {
            // 문제가 없을 경우 받은 이름 그대로 전달
            return fallbackFileName;
        }
        // Todo : 나중에 리눅스 서버에 넣을 경우 수정하기 | 근데 내 서버 쓸거라 안해도 되긴하지 않을까?
        // 받은 내용은 역슬래시(리눅스 등) 일 경우 슬래시(윈도우 기반으로 변경)
        return normalized.replace("\\", "/");
    }

    private Long normalizeLastModified(Long lastModified) {
        if (lastModified == null || lastModified < 0) {
            return 0L;
        }
        return lastModified;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? null : parentId;
    }

    private String normalizeOwnedObjectKey(Long userIdx, String objectKey) {
        String normalized = objectKey == null ? "" : objectKey.trim();
        if (normalized.isEmpty() || !normalized.startsWith(userIdx + "/")) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return normalized;
    }

    private List<String> normalizeOwnedObjectKeys(Long userIdx, List<String> objectKeys) {
        if (objectKeys == null) {
            return List.of();
        }

        return objectKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> normalizeOwnedObjectKey(userIdx, value))
                .distinct()
                .toList();
    }

    private List<String> normalizeAbortTargets(Long userIdx, UploadDto.AbortReq request) {
        if (request == null) {
            return List.of();
        }

        List<String> cleanupTargets = new ArrayList<>();
        if (request.getFinalObjectKey() != null && !request.getFinalObjectKey().isBlank()) {
            String finalObjectKey = normalizeOwnedObjectKey(userIdx, request.getFinalObjectKey());
            boolean persistedFile = fileUpDownloadRepository.findByUser_IdxAndFileSavePath(userIdx, finalObjectKey).isPresent();
            if (!persistedFile) {
                cleanupTargets.add(finalObjectKey);
            }
        }

        cleanupTargets.addAll(normalizeOwnedObjectKeys(userIdx, request.getChunkObjectKeys()));
        return cleanupTargets.stream().distinct().toList();
    }

    private String generatePresignedUploadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .expiry(minioProperties.getPresignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception e) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    private boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureUploadedObjectExists(String objectKey) {
        if (!objectExists(objectKey)) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    private void ensureAllUploaded(List<String> chunkObjectKeys) {
        boolean missingChunk = chunkObjectKeys.stream().anyMatch(objectKey -> !objectExists(objectKey));
        if (missingChunk) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    private void composeFinalObject(String finalObjectKey, List<String> chunkObjectKeys) {
        try {
            List<ComposeSource> sources = chunkObjectKeys.stream()
                    .map(objectKey -> ComposeSource.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build())
                    .toList();

            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(finalObjectKey)
                            .sources(sources)
                            .build()
            );
        } catch (Exception e) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    private void deleteObjectKeys(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        try {
            List<DeleteObject> deleteTargets = objectKeys.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(key -> !key.isBlank())
                    .distinct()
                    .map(DeleteObject::new)
                    .toList();

            if (deleteTargets.isEmpty()) {
                return;
            }

            Iterable<Result<io.minio.messages.DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .objects(deleteTargets)
                            .build()
            );

            for (Result<io.minio.messages.DeleteError> result : results) {
                result.get();
            }
        } catch (Exception ignored) {
        }
    }

    private void saveFinalFileInfo(
            Long userIdx,
            UploadDto.CompleteReq request,
            String fileOriginName,
            String fileFormat,
            long fileSize,
            String finalObjectKey
    ) {
        uploadFolderService.saveCompletedUpload(
                userIdx,
                request,
                fileOriginName,
                fileFormat,
                fileSize,
                finalObjectKey
        );
    }

    private String extractFileSaveName(String finalObjectKey) {
        int separatorIndex = finalObjectKey.lastIndexOf('/');
        if (separatorIndex < 0 || separatorIndex >= finalObjectKey.length() - 1) {
            return finalObjectKey;
        }
        return finalObjectKey.substring(separatorIndex + 1);
    }
    private void recordAbortedUploadBytes(Long userIdx, List<String> objectKeys, String finalObjectKey) {
        long uploadedBytes = sumExistingObjectSizes(objectKeys);
        if (uploadedBytes <= 0L) {
            return;
        }

        storageAnalyticsService.recordIngress(
                userIdx,
                DataTransferSource.DRIVE_UPLOAD,
                DataTransferStatus.ABORTED,
                uploadedBytes,
                finalObjectKey,
                "aborted-upload"
        );
    }

    private long sumExistingObjectSizes(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return 0L;
        }

        return objectKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .mapToLong(this::statObjectSize)
                .sum();
    }

    private long statObjectSize(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return 0L;
        }

        try {
            return Math.max(0L, minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build()
            ).size());
        } catch (Exception exception) {
            return 0L;
        }
    }


    // 업로드 예정 파일의 용량 예약 정보를 저장하는 레코드임
    // finalObjectKey = 최종 저장될 파일의 object key(MinIO저장 경로) ,fileSize = 해당 파일의 크기
    // 용도는 "이 파일이 업로드될 예정이니 이 용량을 예약해 두자" 라는 의미
    private long resolveCompletedObjectSize(String finalObjectKey, long expectedFileSize) {
        long actualFileSize = statObjectSize(finalObjectKey);
        if (actualFileSize > 0L) {
            return actualFileSize;
        }
        return Math.max(0L, expectedFileSize);
    }

    private record PendingUploadReservation(String finalObjectKey, long fileSize) {
    }

    private record UploadReservation(Long userIdx, long reservedBytes, long expiresAtMillis) {
    }
}
