package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAsset;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetDto;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetType;
import com.example.WaffleBear.workspace.model.post.Post;
import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
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

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "heic", "avif", "apng", "jfif", "tif", "tiff"
    );

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
                String objectKey = buildObjectKey(permission.workspace(), objectFolder, storedFileName);
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

    private String buildObjectKey(Post workspace, String objectFolder, String storedFileName) {
        String workspaceFolder = workspace.getUUID() != null && !workspace.getUUID().isBlank()
                ? workspace.getUUID()
                : String.valueOf(workspace.getIdx());

        return "workspace/" + workspaceFolder + "/" + objectFolder + "/" + storedFileName;
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
        String primaryBucket = normalizeBucketName(minioProperties.getBucket_work());
        if (StringUtils.hasText(primaryBucket)) {
            ensureBucketExists(primaryBucket);
            return primaryBucket;
        }

        String fallbackBucket = normalizeBucketName(minioProperties.getBucket_cloud());
        if (!StringUtils.hasText(fallbackBucket)) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        ensureBucketExists(fallbackBucket);
        return fallbackBucket;
    }

    private String normalizeBucketName(String bucketName) {
        return bucketName == null ? "" : bucketName.trim();
    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            }
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private record WorkspacePermission(Post workspace, AccessRole accessRole) {
    }
}
