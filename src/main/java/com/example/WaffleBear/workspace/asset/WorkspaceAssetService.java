package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAsset;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetDto;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetType;
import com.example.WaffleBear.workspace.model.post.Post;
import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceAssetService {

    private static final String DEFAULT_WORKSPACE_BUCKET = "innoutfilebucket";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "heic", "avif", "apng", "jfif", "tif", "tiff"
    );

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final UserRepository userRepository;
    private final WorkspaceAssetRepository workspaceAssetRepository;
    private final UserPostRepository userPostRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<WorkspaceAssetDto.AssetRes> listAssets(Long userIdx, Long workspaceIdx) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, false);

        return workspaceAssetRepository.findAllByWorkspace_IdxOrderByCreatedAtDesc(permission.workspace().getIdx())
                .stream()
                .map(this::toAssetRes)
                .toList();
    }

    @Transactional
    public List<WorkspaceAssetDto.AssetRes> uploadAssets(Long userIdx, Long workspaceIdx, List<MultipartFile> files) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, true);
        if (files == null || files.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        String objectFolder = "asset-" + UUID.randomUUID();
        List<WorkspaceAsset> assetsToSave = new ArrayList<>();
        List<String> uploadedObjectKeys = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                validateFile(file);

                String originalName = sanitizeOriginalName(file.getOriginalFilename());
                String extension = extractExtension(originalName);
                String storedFileName = buildStoredFileName(extension);
                String objectKey = buildObjectKey(permission.workspace(), userIdx, objectFolder, storedFileName);
                String contentType = resolveContentType(file.getContentType());
                WorkspaceAssetType assetType = resolveAssetType(contentType, extension);

                try (InputStream inputStream = file.getInputStream()) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(resolveWorkspaceBucketName())
                                    .object(objectKey)
                                    .stream(inputStream, file.getSize(), -1)
                                    .contentType(contentType)
                                    .build()
                    );
                }

                uploadedObjectKeys.add(objectKey);
                assetsToSave.add(WorkspaceAsset.builder()
                        .workspace(permission.workspace())
                        .uploader(User.builder().idx(userIdx).build())
                        .assetType(assetType)
                        .originalName(originalName)
                        .storedFileName(storedFileName)
                        .objectFolder(objectFolder)
                        .objectKey(objectKey)
                        .contentType(contentType)
                        .fileSize(file.getSize())
                        .build());
            }

            List<WorkspaceAssetDto.AssetRes> savedAssets = workspaceAssetRepository.saveAll(assetsToSave)
                    .stream()
                    .map(this::toAssetRes)
                    .toList();
            publishAssetEvent(permission.workspace().getIdx(), "UPSERT", userIdx, savedAssets, savedAssets.stream()
                    .map(WorkspaceAssetDto.AssetRes::idx)
                    .toList());
            return savedAssets;
        } catch (BaseException exception) {
            deleteObjectKeys(uploadedObjectKeys);
            throw exception;
        } catch (Exception exception) {
            deleteObjectKeys(uploadedObjectKeys);
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    @Transactional
    public WorkspaceAssetDto.ActionRes deleteAsset(Long userIdx, Long workspaceIdx, Long assetIdx) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, true);
        WorkspaceAsset asset = workspaceAssetRepository.findByIdxAndWorkspace_Idx(assetIdx, permission.workspace().getIdx())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        deleteObjectKeys(List.of(asset.getObjectKey()));
        workspaceAssetRepository.delete(asset);
        publishAssetEvent(permission.workspace().getIdx(), "DELETE", userIdx, List.of(), List.of(asset.getIdx()));

        return new WorkspaceAssetDto.ActionRes(asset.getIdx(), "delete");
    }

    @Transactional
    public FileCommonDto.FileListItemRes saveAssetToDrive(Long userIdx, Long workspaceIdx, Long assetIdx, Long parentId) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, false);
        WorkspaceAsset asset = workspaceAssetRepository.findByIdxAndWorkspace_Idx(assetIdx, permission.workspace().getIdx())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        FileInfo parentFolder = resolveParentFolder(userIdx, parentId);
        String sourceBucket = resolveWorkspaceBucketName();
        String targetBucket = resolveDriveBucketName();
        String fileFormat = resolveDriveFileFormat(asset);
        String savedFileName = buildDriveStoredFileName(fileFormat);
        String savedObjectKey = userIdx + "/" + savedFileName;

        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(targetBucket)
                            .object(savedObjectKey)
                            .source(CopySource.builder()
                                    .bucket(sourceBucket)
                                    .object(asset.getObjectKey())
                                    .build())
                            .build()
            );
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        FileInfo savedFile = fileUpDownloadRepository.save(
                FileInfo.builder()
                        .user(User.builder().idx(userIdx).build())
                        .parent(parentFolder)
                        .nodeType(FileNodeType.FILE)
                        .fileOriginName(asset.getOriginalName())
                        .fileFormat(fileFormat)
                        .fileSaveName(savedFileName)
                        .fileSavePath(savedObjectKey)
                        .fileSize(asset.getFileSize())
                        .lockedFile(false)
                        .sharedFile(false)
                        .trashed(false)
                        .deletedAt(null)
                        .build()
        );

        return toDriveFileListItem(savedFile, targetBucket);
    }

    @Transactional
    public void deleteAllWorkspaceAssets(Post workspace) {
        if (workspace == null || workspace.getIdx() == null) {
            return;
        }

        List<WorkspaceAsset> assets = workspaceAssetRepository.findAllByWorkspace_Idx(workspace.getIdx());
        if (assets.isEmpty()) {
            return;
        }

        deleteObjectKeys(assets.stream().map(WorkspaceAsset::getObjectKey).toList());
        workspaceAssetRepository.deleteAllInBatch(assets);
    }

    private WorkspacePermission requireWorkspaceAccess(Long userIdx, Long workspaceIdx, boolean writeRequired) {
        if (userIdx == null || userIdx <= 0 || workspaceIdx == null || workspaceIdx <= 0) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        UserPost userPost = userPostRepository.findByUser_IdxAndWorkspace_Idx(userIdx, workspaceIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        if (writeRequired && userPost.getLevel() == AccessRole.READ) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return new WorkspacePermission(userPost.getWorkspace(), userPost.getLevel());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        if (originalName.length() > 255) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_LENGTH_WRONG);
        }
    }

    private String sanitizeOriginalName(String originalName) {
        String normalized = originalName == null ? "" : originalName.trim().replace("\\", "/");
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(slashIndex + 1);
        }

        if (normalized.isBlank() || normalized.contains("\u0000")) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        return normalized;
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDot + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String buildStoredFileName(String extension) {
        String normalizedExtension = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        return normalizedExtension.isBlank()
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + normalizedExtension;
    }

    private String buildDriveStoredFileName(String extension) {
        String normalizedExtension = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        return normalizedExtension.isBlank()
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + normalizedExtension;
    }

    private String resolveContentType(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim();
        return normalized.isBlank() ? "application/octet-stream" : normalized;
    }

    private WorkspaceAssetType resolveAssetType(String contentType, String extension) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return WorkspaceAssetType.IMAGE;
        }

        return IMAGE_EXTENSIONS.contains(extension) ? WorkspaceAssetType.IMAGE : WorkspaceAssetType.FILE;
    }

    private String buildObjectKey(Post workspace, Long userIdx, String objectFolder, String storedFileName) {
        String userFolder = resolveWorkspaceUserFolder(userIdx);
        String workspaceFolder = workspace.getUUID() != null && !workspace.getUUID().isBlank()
                ? sanitizeFolderSegment(workspace.getUUID())
                : String.valueOf(workspace.getIdx());

        return "workspace/" + userFolder + "/" + workspaceFolder + "/" + objectFolder + "/" + storedFileName;
    }

    private String resolveDriveFileFormat(WorkspaceAsset asset) {
        String originalName = asset == null ? null : asset.getOriginalName();
        String extension = extractExtension(originalName == null ? "" : originalName);
        if (!extension.isBlank()) {
            return extension;
        }

        String storedFileName = asset == null ? null : asset.getStoredFileName();
        extension = extractExtension(storedFileName == null ? "" : storedFileName);
        return extension.isBlank() ? "bin" : extension;
    }

    private WorkspaceAssetDto.AssetRes toAssetRes(WorkspaceAsset asset) {
        String downloadUrl = generatePresignedGetUrl(asset.getObjectKey());
        String previewUrl = asset.getAssetType() == WorkspaceAssetType.IMAGE ? downloadUrl : null;

        return new WorkspaceAssetDto.AssetRes(
                asset.getIdx(),
                asset.getWorkspace() != null ? asset.getWorkspace().getIdx() : null,
                asset.getAssetType().name(),
                asset.getOriginalName(),
                asset.getStoredFileName(),
                asset.getObjectFolder(),
                asset.getObjectKey(),
                asset.getContentType(),
                asset.getFileSize(),
                previewUrl,
                downloadUrl,
                minioProperties.getPresignedUrlExpirySeconds(),
                asset.getCreatedAt()
        );
    }

    private String generatePresignedGetUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(resolveWorkspaceBucketName())
                            .object(objectKey)
                            .expiry(minioProperties.getPresignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private void deleteObjectKeys(Collection<String> objectKeys) {
        List<DeleteObject> deleteTargets = objectKeys == null
                ? List.of()
                : objectKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .map(DeleteObject::new)
                .toList();

        if (deleteTargets.isEmpty()) {
            return;
        }

        try {
            Iterable<Result<io.minio.messages.DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(resolveWorkspaceBucketName())
                            .objects(deleteTargets)
                            .build()
            );

            for (Result<io.minio.messages.DeleteError> result : results) {
                result.get();
            }
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private void publishAssetEvent(Long workspaceIdx, String action, Long actorUserIdx, List<WorkspaceAssetDto.AssetRes> assets, List<Long> assetIdxList) {
        if (workspaceIdx == null) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/sub/workspace/assets/" + workspaceIdx,
                new WorkspaceAssetDto.AssetEvent(
                        workspaceIdx,
                        action,
                        actorUserIdx,
                        assets == null ? List.of() : assets,
                        assetIdxList == null ? List.of() : assetIdxList
                )
        );
    }

    private String resolveWorkspaceBucketName() {
        return DEFAULT_WORKSPACE_BUCKET;
    }

    private String resolveDriveBucketName() {
        return resolveWorkspaceBucketName();
    }

    private FileInfo resolveParentFolder(Long userIdx, Long parentId) {
        if (parentId == null) {
            return null;
        }

        FileInfo parent = fileUpDownloadRepository.findByIdxAndUser_Idx(parentId, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        if (parent.isTrashed() || parent.getNodeType() != FileNodeType.FOLDER) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return parent;
    }

    private String resolveWorkspaceUserFolder(Long userIdx) {
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        String userName = user.getName();
        if (!StringUtils.hasText(userName)) {
            userName = user.getEmail();
        }

        if (!StringUtils.hasText(userName)) {
            userName = "user-" + userIdx;
        }

        return sanitizeFolderSegment(userName);
    }

    private String sanitizeFolderSegment(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        normalized = normalized.replace("\\", "-").replace("/", "-");
        normalized = normalized.replaceAll("[^0-9A-Za-z가-힣._-]", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^[-.]+|[-.]+$", "");

        if (!StringUtils.hasText(normalized)) {
            return "workspace";
        }

        return normalized;
    }

    private FileCommonDto.FileListItemRes toDriveFileListItem(FileInfo entity, String bucketName) {
        return FileCommonDto.FileListItemRes.builder()
                .idx(entity.getIdx())
                .fileOriginName(entity.getFileOriginName())
                .fileSaveName(entity.getFileSaveName())
                .fileSavePath(entity.getFileSavePath())
                .fileFormat(entity.getFileFormat())
                .fileSize(entity.getFileSize())
                .nodeType(FileNodeType.FILE.name())
                .parentId(entity.getParent() != null ? entity.getParent().getIdx() : null)
                .lockedFile(entity.isLockedFile())
                .sharedFile(entity.isSharedFile())
                .trashed(entity.isTrashed())
                .deletedAt(entity.getDeletedAt())
                .uploadDate(entity.getUploadDate())
                .lastModifyDate(entity.getLastModifyDate())
                .presignedDownloadUrl(generateDrivePresignedGetUrl(entity.getFileSavePath(), bucketName))
                .thumbnailPresignedUrl(null)
                .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                .build();
    }

    private String generateDrivePresignedGetUrl(String objectKey, String bucketName) {
        if (!StringUtils.hasText(objectKey) || !StringUtils.hasText(bucketName)) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(minioProperties.getPresignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception exception) {
            return null;
        }
    }

    private record WorkspacePermission(Post workspace, AccessRole accessRole) {
    }
}
