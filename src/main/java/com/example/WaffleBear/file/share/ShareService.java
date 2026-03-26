package com.example.WaffleBear.file.share;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.file.share.model.ShareDto;
import com.example.WaffleBear.file.share.model.FileShare;
import com.example.WaffleBear.file.service.StoragePlanService;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private static final int MAX_TEXT_PREVIEW_BYTES = 64 * 1024;
    private static final String THUMBNAIL_DIRECTORY_NAME = "thumbnails";

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final ShareRepository shareRepository;
    private final UserRepository userRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final StoragePlanService storagePlanService;
    private final NotificationService notificationService;

    public List<ShareDto.SharedFileRes> sharedFileList(Long userIdx) {
        requireAuthenticated(userIdx);

        return shareRepository.findAllByRecipient_IdxOrderByCreatedAtDesc(userIdx)
                .stream()
                .filter(share -> share.getFile() != null)
                .filter(share -> !share.getFile().isTrashed())
                .map(this::toSharedFileRes)
                .toList();
    }

    public List<ShareDto.SentSharedFileRes> sentSharedFileList(Long userIdx) {
        requireAuthenticated(userIdx);

        Map<Long, List<FileShare>> sharesByFileId = new LinkedHashMap<>();

        shareRepository.findAllByOwner_IdxOrderByCreatedAtDesc(userIdx)
                .stream()
                .filter(share -> share.getFile() != null)
                .filter(share -> !share.getFile().isTrashed())
                .forEach(share -> sharesByFileId
                        .computeIfAbsent(share.getFile().getIdx(), ignored -> new ArrayList<>())
                        .add(share));

        return sharesByFileId.values().stream()
                .map(this::toSentSharedFileRes)
                .toList();
    }

    public List<ShareDto.ShareInfoRes> getShareInfo(Long userIdx, Long fileIdx) {
        FileInfo file = getOwnedFile(userIdx, fileIdx);

        return shareRepository.findAllByFile_Idx(file.getIdx())
                .stream()
                .sorted(Comparator.comparing(FileShare::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toShareInfo)
                .toList();
    }

    public FileCommonDto.FileActionRes shareFiles(Long userIdx, List<Long> fileIdxList, String recipientEmail) {
        requireAuthenticated(userIdx);
        StoragePlanService.StorageQuota storageQuota = storagePlanService.resolveQuota(userIdx);
        if (!storageQuota.shareEnabled()) {
            throw BaseException.from(BaseResponseStatus.PLAN_FEATURE_NOT_AVAILABLE);
        }
        if (fileIdxList == null || fileIdxList.isEmpty() || recipientEmail == null || recipientEmail.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String normalizedEmail = recipientEmail.trim().toLowerCase(Locale.ROOT);
        User recipient = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        if (Objects.equals(recipient.getIdx(), userIdx)) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<FileInfo> newlySharedFiles = fileIdxList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(fileIdx -> {
                    FileInfo file = getOwnedFile(userIdx, fileIdx);
                    ensureShareableFile(file);

                    if (shareRepository.findByFile_IdxAndRecipient_Idx(fileIdx, recipient.getIdx()).isPresent()) {
                        return null;
                    }

                    shareRepository.save(FileShare.builder()
                            .file(file)
                            .owner(file.getUser())
                            .recipient(recipient)
                            .build());
                    file.changeSharedFile(true);
                    fileUpDownloadRepository.save(file);
                    return file;
                })
                .filter(Objects::nonNull)
                .toList();

        if (!newlySharedFiles.isEmpty()) {
            notificationService.sendGeneralNotification(
                    recipient.getIdx(),
                    "파일 공유",
                    buildFileShareMessage(newlySharedFiles)
            );
        }

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(null)
                .action("share")
                .affectedCount(newlySharedFiles.size())
                .build();
    }

    public int shareFilesToUsers(Long userIdx, List<Long> fileIdxList, Collection<Long> recipientUserIds) {
        requireAuthenticated(userIdx);
        StoragePlanService.StorageQuota storageQuota = storagePlanService.resolveQuota(userIdx);
        if (!storageQuota.shareEnabled()) {
            throw BaseException.from(BaseResponseStatus.PLAN_FEATURE_NOT_AVAILABLE);
        }
        if (fileIdxList == null || fileIdxList.isEmpty() || recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<User> recipients = recipientUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(recipientUserId -> userRepository.findById(recipientUserId)
                        .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND)))
                .filter(recipient -> !Objects.equals(recipient.getIdx(), userIdx))
                .toList();

        if (recipients.isEmpty()) {
            return 0;
        }

        int affectedCount = 0;

        for (User recipient : recipients) {
            List<FileInfo> newlySharedFiles = fileIdxList.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(fileIdx -> {
                        FileInfo file = getOwnedFile(userIdx, fileIdx);
                        ensureShareableFile(file);

                        if (shareRepository.findByFile_IdxAndRecipient_Idx(fileIdx, recipient.getIdx()).isPresent()) {
                            return null;
                        }

                        shareRepository.save(FileShare.builder()
                                .file(file)
                                .owner(file.getUser())
                                .recipient(recipient)
                                .build());
                        file.changeSharedFile(true);
                        fileUpDownloadRepository.save(file);
                        return file;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            affectedCount += newlySharedFiles.size();

            if (!newlySharedFiles.isEmpty()) {
                notificationService.sendGeneralNotification(
                        recipient.getIdx(),
                        "파일 공유",
                        buildFileShareMessage(newlySharedFiles)
                );
            }
        }

        return affectedCount;
    }

    public FileCommonDto.FileActionRes cancelShare(Long userIdx, List<Long> fileIdxList, String recipientEmail) {
        requireAuthenticated(userIdx);
        if (fileIdxList == null || fileIdxList.isEmpty() || recipientEmail == null || recipientEmail.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String normalizedEmail = recipientEmail.trim().toLowerCase(Locale.ROOT);
        int affectedCount = 0;

        for (Long fileIdx : fileIdxList.stream().filter(Objects::nonNull).distinct().toList()) {
            FileInfo file = getOwnedFile(userIdx, fileIdx);
            boolean removed = false;

            Optional<FileShare> share = shareRepository.findByFile_IdxAndRecipient_Email(fileIdx, normalizedEmail);
            if (share.isPresent()) {
                shareRepository.delete(share.get());
                removed = true;
            }

            long remain = shareRepository.countByFile_Idx(file.getIdx());
            file.changeSharedFile(remain > 0);
            fileUpDownloadRepository.save(file);
            if (removed) {
                affectedCount += 1;
            }
        }

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(null)
                .action("cancel-share")
                .affectedCount(affectedCount)
                .build();
    }

    public FileCommonDto.FileActionRes cancelAllShares(Long userIdx, List<Long> fileIdxList) {
        requireAuthenticated(userIdx);
        if (fileIdxList == null || fileIdxList.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        int affectedCount = 0;

        for (Long fileIdx : fileIdxList.stream().filter(Objects::nonNull).distinct().toList()) {
            FileInfo file = getOwnedFile(userIdx, fileIdx);
            List<FileShare> shares = shareRepository.findAllByFile_Idx(file.getIdx());

            if (!shares.isEmpty()) {
                shareRepository.deleteAll(shares);
                affectedCount += shares.size();
            }

            file.changeSharedFile(false);
            fileUpDownloadRepository.save(file);
        }

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(null)
                .action("cancel-all-share")
                .affectedCount(affectedCount)
                .build();
    }

    public FileCommonDto.FileListItemRes saveSharedFileToDrive(Long userIdx, Long fileIdx, Long parentId) {
        requireAuthenticated(userIdx);
        FileShare share = getSharedRecord(userIdx, fileIdx);
        FileInfo original = share.getFile();
        ensureAccessibleSharedFile(original);

        FileInfo parent = resolveParentFolder(userIdx, parentId);
        String objectKey = original.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String newSaveName = UUID.randomUUID() + "." + original.getFileFormat();
        String newObjectKey = userIdx + "/" + newSaveName;

        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(newObjectKey)
                            .source(CopySource.builder()
                                    .bucket(minioProperties.getBucket_cloud())
                                    .object(objectKey)
                                    .build())
                            .build()
            );
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        FileInfo copy = FileInfo.builder()
                .user(User.builder().idx(userIdx).build())
                .parent(parent)
                .nodeType(FileNodeType.FILE)
                .fileOriginName(original.getFileOriginName())
                .fileFormat(original.getFileFormat())
                .fileSaveName(newSaveName)
                .fileSavePath(newObjectKey)
                .fileSize(original.getFileSize())
                .lockedFile(false)
                .sharedFile(false)
                .trashed(false)
                .deletedAt(null)
                .build();

        return toFileListItem(fileUpDownloadRepository.save(copy));
    }

    public FileInfoDto.TextPreviewRes getSharedTextPreview(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        FileInfo file = getSharedRecord(userIdx, fileIdx).getFile();
        ensureAccessibleSharedFile(file);

        if (!isTextPreviewable(file.getFileFormat())) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        try (var objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket_cloud())
                        .object(file.getFileSavePath())
                        .build()
        )) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int totalRead = 0;
            boolean truncated = false;
            int read;

            while ((read = objectStream.read(buffer)) != -1) {
                int writable = Math.min(read, MAX_TEXT_PREVIEW_BYTES - totalRead);
                if (writable > 0) {
                    outputStream.write(buffer, 0, writable);
                    totalRead += writable;
                }
                if (totalRead >= MAX_TEXT_PREVIEW_BYTES) {
                    truncated = true;
                    break;
                }
            }

            return FileInfoDto.TextPreviewRes.builder()
                    .idx(file.getIdx())
                    .fileOriginName(file.getFileOriginName())
                    .fileFormat(file.getFileFormat())
                    .contentType(resolveTextContentType(file.getFileFormat()))
                    .content(outputStream.toString(StandardCharsets.UTF_8))
                    .truncated(truncated)
                    .fileSize(file.getFileSize())
                    .build();
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    public FileCommonDto.FileDownloadPayload downloadSharedFile(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        FileInfo file = getSharedRecord(userIdx, fileIdx).getFile();
        ensureAccessibleSharedFile(file);

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return new FileCommonDto.FileDownloadPayload(
                readObjectBytes(minioProperties.getBucket_cloud(), objectKey),
                resolveDownloadContentType(file.getFileOriginName()),
                sanitizeDownloadFileName(file.getFileOriginName(), file.getFileSaveName()),
                file.getFileSize()
        );
    }

    public String getSharedFileDownloadUrl(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        FileInfo file = getSharedRecord(userIdx, fileIdx).getFile();
        ensureAccessibleSharedFile(file);

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return generateAttachmentDownloadUrl(
                objectKey,
                sanitizeDownloadFileName(file.getFileOriginName(), file.getFileSaveName()),
                resolveDownloadContentType(file.getFileOriginName())
        );
    }

    private void requireAuthenticated(Long userIdx) {
        if (userIdx == null || userIdx <= 0L) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private FileInfo getOwnedFile(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        if (fileIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return fileUpDownloadRepository.findByIdxAndUser_Idx(fileIdx, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
    }

    private FileShare getSharedRecord(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        if (fileIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return shareRepository.findByFile_IdxAndRecipient_Idx(fileIdx, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
    }

    private void ensureShareableFile(FileInfo file) {
        if (file == null || file.isTrashed() || file.isLockedFile() || resolveNodeType(file) != FileNodeType.FILE) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private void ensureAccessibleSharedFile(FileInfo file) {
        if (file == null || file.isTrashed() || file.isLockedFile() || resolveNodeType(file) != FileNodeType.FILE) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private FileInfo resolveParentFolder(Long userIdx, Long parentId) {
        if (parentId == null) {
            return null;
        }

        FileInfo parent = getOwnedFile(userIdx, parentId);
        if (resolveNodeType(parent) != FileNodeType.FOLDER || parent.isTrashed()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return parent;
    }

    private ShareDto.ShareInfoRes toShareInfo(FileShare share) {
        return new ShareDto.ShareInfoRes(
                share.getIdx(),
                share.getFile() != null ? share.getFile().getIdx() : null,
                share.getFile() != null ? share.getFile().getFileOriginName() : null,
                share.getOwner() != null ? share.getOwner().getName() : null,
                share.getOwner() != null ? share.getOwner().getEmail() : null,
                share.getRecipient() != null ? share.getRecipient().getName() : null,
                share.getRecipient() != null ? share.getRecipient().getEmail() : null,
                share.getCreatedAt()
        );
    }

    private ShareDto.SharedFileRes toSharedFileRes(FileShare share) {
        FileInfo file = share.getFile();
        return new ShareDto.SharedFileRes(
                file.getIdx(),
                file.getFileOriginName(),
                file.getFileSaveName(),
                file.getFileSavePath(),
                file.getFileFormat(),
                file.getFileSize(),
                resolveNodeType(file).name(),
                file.getParent() != null ? file.getParent().getIdx() : null,
                file.isLockedFile(),
                file.isSharedFile(),
                file.isTrashed(),
                file.getDeletedAt(),
                file.getUploadDate(),
                file.getLastModifyDate(),
                generatePresignedDownloadUrl(file),
                generatePresignedThumbnailUrl(file),
                minioProperties.getPresignedUrlExpirySeconds(),
                true,
                share.getOwner() != null ? share.getOwner().getName() : null,
                share.getOwner() != null ? share.getOwner().getEmail() : null,
                share.getCreatedAt()
        );
    }

    private ShareDto.SentSharedFileRes toSentSharedFileRes(List<FileShare> shares) {
        FileShare firstShare = shares.get(0);
        FileInfo file = firstShare.getFile();
        List<ShareDto.ShareRecipientRes> recipients = shares.stream()
                .map(share -> new ShareDto.ShareRecipientRes(
                        share.getRecipient() != null ? share.getRecipient().getName() : null,
                        share.getRecipient() != null ? share.getRecipient().getEmail() : null,
                        share.getCreatedAt()
                ))
                .toList();

        return new ShareDto.SentSharedFileRes(
                file.getIdx(),
                file.getFileOriginName(),
                file.getFileSaveName(),
                file.getFileSavePath(),
                file.getFileFormat(),
                file.getFileSize(),
                resolveNodeType(file).name(),
                file.getParent() != null ? file.getParent().getIdx() : null,
                file.isLockedFile(),
                file.isSharedFile(),
                file.isTrashed(),
                file.getDeletedAt(),
                file.getUploadDate(),
                file.getLastModifyDate(),
                generatePresignedDownloadUrl(file),
                generatePresignedThumbnailUrl(file),
                minioProperties.getPresignedUrlExpirySeconds(),
                false,
                firstShare.getOwner() != null ? firstShare.getOwner().getName() : null,
                firstShare.getOwner() != null ? firstShare.getOwner().getEmail() : null,
                firstShare.getCreatedAt(),
                recipients.size(),
                recipients
        );
    }

    private String buildFileShareMessage(List<FileInfo> files) {
        FileInfo firstFile = files.get(0);
        String ownerName = firstFile.getUser() != null ? firstFile.getUser().getName() : "알 수 없는 사용자";

        if (files.size() == 1) {
            return ownerName + " 님이 '" + firstFile.getFileOriginName() + "' 파일을 공유했습니다.";
        }

        return ownerName + " 님이 '" + firstFile.getFileOriginName() + "' 외 " + (files.size() - 1) + "개 파일을 공유했습니다.";
    }

    private FileCommonDto.FileListItemRes toFileListItem(FileInfo entity) {
        return FileCommonDto.FileListItemRes.builder()
                .idx(entity.getIdx())
                .fileOriginName(entity.getFileOriginName())
                .fileSaveName(entity.getFileSaveName())
                .fileSavePath(entity.getFileSavePath())
                .fileFormat(entity.getFileFormat())
                .fileSize(entity.getFileSize())
                .nodeType(resolveNodeType(entity).name())
                .parentId(entity.getParent() != null ? entity.getParent().getIdx() : null)
                .lockedFile(entity.isLockedFile())
                .sharedFile(entity.isSharedFile())
                .trashed(entity.isTrashed())
                .deletedAt(entity.getDeletedAt())
                .uploadDate(entity.getUploadDate())
                .lastModifyDate(entity.getLastModifyDate())
                .presignedDownloadUrl(generatePresignedDownloadUrl(entity))
                .thumbnailPresignedUrl(generatePresignedThumbnailUrl(entity))
                .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                .build();
    }

    private String generatePresignedDownloadUrl(FileInfo entity) {
        if (entity == null || entity.isLockedFile() || resolveNodeType(entity) != FileNodeType.FILE) {
            return null;
        }

        String objectKey = entity.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build());
        } catch (Exception exception) {
            return null;
        }
    }

    private String generatePresignedThumbnailUrl(FileInfo entity) {
        if (entity == null || entity.isLockedFile() || resolveNodeType(entity) != FileNodeType.FILE || !isVideoFile(entity.getFileFormat())) {
            return null;
        }

        String objectKey = entity.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String thumbnailObjectKey = buildThumbnailObjectKey(objectKey);
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket_cloud())
                    .object(thumbnailObjectKey)
                    .build());
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(thumbnailObjectKey)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build());
        } catch (Exception exception) {
            return null;
        }
    }

    private String buildThumbnailObjectKey(String objectKey) {
        int pathSeparatorIndex = objectKey.lastIndexOf('/');
        String directory = pathSeparatorIndex >= 0 ? objectKey.substring(0, pathSeparatorIndex + 1) : "";
        String fileName = pathSeparatorIndex >= 0 ? objectKey.substring(pathSeparatorIndex + 1) : objectKey;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;

        return directory + THUMBNAIL_DIRECTORY_NAME + "/" + baseName + ".jpg";
    }

    private boolean isVideoFile(String fileFormat) {
        String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);
        return Set.of("mp4", "mov", "avi", "mkv", "wmv", "webm", "m4v", "mpeg", "mpg", "ogv", "3gp").contains(extension);
    }

    private boolean isTextPreviewable(String fileFormat) {
        String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);
        return Set.of(
                "txt", "md", "csv", "log", "json", "xml", "html", "htm",
                "css", "js", "ts", "java", "py", "sql", "yml", "yaml",
                "properties", "sh", "bat"
        ).contains(extension);
    }

private String resolveTextContentType(String fileFormat) {
    String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);

        if (Set.of("json").contains(extension)) {
            return "application/json";
        }
        if (Set.of("html", "htm").contains(extension)) {
            return "text/html";
        }
        if (Set.of("xml").contains(extension)) {
            return "application/xml";
        }
        if (Set.of("csv").contains(extension)) {
            return "text/csv";
    }
    return "text/plain";
}

private byte[] readObjectBytes(String bucketName, String objectKey) {
    try (var objectStream = minioClient.getObject(
            GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
    )) {
        return objectStream.readAllBytes();
    } catch (Exception exception) {
        throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
    }
}

private String generateAttachmentDownloadUrl(String objectKey, String fileName, String contentType) {
    if (objectKey == null || objectKey.isBlank()) {
        throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
    }

    try {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put(
                "response-content-disposition",
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );
        queryParams.put(
                "response-content-type",
                (contentType == null || contentType.isBlank())
                        ? resolveDownloadContentType(fileName)
                        : contentType
        );

        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioProperties.getBucket_cloud())
                .object(objectKey)
                .expiry(minioProperties.getPresignedUrlExpirySeconds())
                .extraQueryParams(queryParams)
                .build());
    } catch (Exception exception) {
        throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
    }
}

private String resolveDownloadContentType(String fileName) {
    String contentType = URLConnection.guessContentTypeFromName(fileName == null ? "" : fileName);
    return (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
}

private String sanitizeDownloadFileName(String preferredName, String fallbackName) {
    String candidate = preferredName;
    if (candidate == null || candidate.isBlank()) {
        candidate = fallbackName;
    }
    if (candidate == null || candidate.isBlank()) {
        candidate = "file";
    }

    return candidate
            .replace("\r", "")
            .replace("\n", "")
            .trim();
}

private FileNodeType resolveNodeType(FileInfo entity) {
    return entity.getNodeType() == null ? FileNodeType.FILE : entity.getNodeType();
}
}
