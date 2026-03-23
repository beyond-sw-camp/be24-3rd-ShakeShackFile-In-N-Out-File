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
import com.example.WaffleBear.workspace.repository.PostRepository;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkspaceAssetService {

    // ─── 하드코딩 버킷명 제거 ──────────────────────────────────────────────────
    // 버킷명은 MinioProperties 에서 주입받습니다.
    // 워크스페이스 에셋 → bucket_work
    // 드라이브 저장     → bucket_cloud  (FileUpDownloadMinioService 와 동일)

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "heic", "avif", "apng", "jfif", "tif", "tiff"
    );
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;   // 5MB
    private static final long MAX_FILE_SIZE = 30L * 1024 * 1024;   // 30MB
    public record EditorJsUploadResult(Long assetIdx, String fileUrl) {}
    private record WorkspacePermission(Post workspace, AccessRole accessRole) {}


    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final UserRepository userRepository;
    private final WorkspaceAssetRepository workspaceAssetRepository;
    private final UserPostRepository userPostRepository;
    private final PostRepository postRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 여러 파일을 한번에 업로드 (일반 에셋용)
     */
    @Transactional
    public List<WorkspaceAssetDto.AssetRes> uploadWorkspaceAssets(
            Long userIdx,
            Long workspaceIdx,
            MultipartFile[] files) {

        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, true);

        if (files == null || files.length == 0) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        Post workspace = permission.workspace();

        // ✅ User 엔티티 제대로 로드
        User uploader = userRepository.findById(userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));

        List<WorkspaceAsset> savedAssets = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            validateFile(file);

            // 용량 및 타입 체크
            String contentType = file.getContentType();
            String originalName = file.getOriginalFilename();
            String extension = extractExtension(originalName == null ? "" : originalName);

            // ❌ 이미지 파일 거부
            boolean isImage = isImageFile(contentType, extension);
            if (isImage) {
                throw new IllegalArgumentException(
                        "이미지 파일은 업로드할 수 없습니다.\n" +
                                "파일: " + originalName + "\n" +
                                "이미지는 에디터 이미지 업로드를 사용해주세요."
                );
            }

            // ✅ 일반 파일 크기 체크 (30MB)
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "파일은 30MB 이하만 업로드 가능합니다.\n" +
                                "파일: " + originalName + "\n" +
                                "크기: " + (file.getSize() / 1024 / 1024) + "MB"
                );
            }

            // Minio 업로드
            String objectKey = "file/" + workspace.getUUID() + "/"
                    + System.currentTimeMillis() + "_" + originalName;

            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucket_cloud())
                                .object(objectKey)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(contentType != null ? contentType : "application/octet-stream")
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
            }

            // ✅ DB 저장 (User를 제대로 로드해서 저장)
            String storedFileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
            WorkspaceAssetType assetType = WorkspaceAssetType.FILE;

            try {
                WorkspaceAsset saved = workspaceAssetRepository.save(
                        WorkspaceAsset.builder()
                                .workspace(workspace)
                                .uploader(uploader)  // ✅ 제대로 로드된 User 객체
                                .assetType(assetType)
                                .originalName(originalName)
                                .storedFileName(storedFileName)
                                .objectFolder("file/" + workspace.getUUID())
                                .objectKey(objectKey)
                                .contentType(contentType != null ? contentType : "application/octet-stream")
                                .fileSize(file.getSize())
                                .build()
                );

                savedAssets.add(saved);

                System.out.println("✅ DB 저장 성공: " + saved.getIdx() + " - " + originalName);
            } catch (Exception e) {
                System.err.println("❌ DB 저장 실패: " + e.getMessage());
                e.printStackTrace();
                // 일단 로깅만 하고 다음 파일 처리
                throw new RuntimeException("파일 DB 저장 실패: " + originalName + " - " + e.getMessage());
            }
        }

        List<WorkspaceAssetDto.AssetRes> result = savedAssets.stream()
                .map(this::toAssetRes)
                .toList();

        publishAssetEvent(workspaceIdx, "UPLOAD", userIdx, result, null);

        return result;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceAssetDto.AssetRes> listAssets(Long userIdx, Long workspaceIdx) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, false);

        return workspaceAssetRepository.findAllByWorkspace_IdxOrderByCreatedAtDesc(permission.workspace().getIdx())
                .stream()
                .map(this::toAssetRes)
                .toList();
    }

    @Transactional
    public EditorJsUploadResult uploadAssetsEditorJs(Long userIdx, Long workspaceIdx, MultipartFile image) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, true);
        if (image == null || image.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }
        String contentType = image.getContentType();
        boolean isImage = IMAGE_EXTENSIONS.contains(contentType);

        if(permission.accessRole == AccessRole.READ) {
            throw new RuntimeException("읽기만 가능합니다.");
        }
        // 용량 체크
        if (isImage && image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 업로드 가능합니다.");
        }
        if (!isImage && image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일은 30MB 이하만 업로드 가능합니다.");
        }
        Post result = postRepository.findById(workspaceIdx).orElseThrow(
                () -> new RuntimeException("해당 워크스페이스가 존재하지않습니다.")
        );
        String objectKey = "asset/" + result.getUUID() + "/"
                + System.currentTimeMillis() + "_" + image.getOriginalFilename();

        // ✅ 1. Minio 업로드
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .stream(image.getInputStream(), image.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }

        // ✅ 2. DB 저장
        String originalName   = image.getOriginalFilename();
        String extension      = extractExtension(originalName == null ? "" : originalName);
        String storedFileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        WorkspaceAssetType assetType = resolveAssetType(contentType, extension);

        WorkspaceAsset saved = workspaceAssetRepository.save(
                WorkspaceAsset.builder()
                        .workspace(result)
                        .uploader(User.builder().idx(userIdx).build())
                        .assetType(assetType)
                        .originalName(originalName)
                        .storedFileName(storedFileName)
                        .objectFolder("asset/" + result.getUUID())
                        .objectKey(objectKey)
                        .contentType(contentType)
                        .fileSize(image.getSize())
                        .build()
        );

        // ✅ 3. presigned URL + assetIdx 반환
        try {
            String fileUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .expiry(60 * 60 * 24)
                            .build()
            );
            return new EditorJsUploadResult(saved.getIdx(), fileUrl);
        } catch (Exception e) {
            throw new RuntimeException("URL 생성 실패: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteEditorJsImage(Long userIdx, Long workspaceIdx, Long assetIdx) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, true);

        WorkspaceAsset asset = workspaceAssetRepository
                .findByIdxAndWorkspace_Idx(assetIdx, permission.workspace().getIdx())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        // ✅ 업로드한 버킷(bucket_cloud)에서 삭제
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(asset.getObjectKey())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage());
        }

        // ✅ DB에서도 삭제
        workspaceAssetRepository.delete(asset);
    }
    /**
     * 일반 에셋 삭제
     */
    @Transactional
    public void deleteWorkspaceAsset(Long userIdx, Long workspaceIdx, Long assetId) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, true);

        WorkspaceAsset asset = workspaceAssetRepository
                .findByIdxAndWorkspace_Idx(assetId, permission.workspace().getIdx())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        // Minio에서 삭제
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(asset.getObjectKey())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage());
        }

        // DB에서 삭제
        workspaceAssetRepository.delete(asset);

        publishAssetEvent(workspaceIdx, "DELETE", userIdx, null, List.of(assetId));
    }

    @Transactional
    public FileCommonDto.FileListItemRes saveAssetToDrive(Long userIdx, Long workspaceIdx, Long assetIdx, Long parentId) {
        WorkspacePermission permission = requireWorkspaceAccess(userIdx, workspaceIdx, false);
        WorkspaceAsset asset = workspaceAssetRepository
                .findByIdxAndWorkspace_Idx(assetIdx, permission.workspace().getIdx())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        FileInfo parentFolder  = resolveParentFolder(userIdx, parentId);
        String targetBucket    = resolveDriveBucketName();       // bucket_cloud
        String fileFormat      = resolveDriveFileFormat(asset);
        String savedFileName   = buildDriveStoredFileName(fileFormat);
        String savedObjectKey  = userIdx + "/" + savedFileName;

        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(targetBucket)
                            .object(savedObjectKey)
                            .source(CopySource.builder()
                                    .bucket(minioProperties.getBucket_cloud())
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

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────────────

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
        String extension    = extractExtension(originalName == null ? "" : originalName);
        if (!extension.isBlank()) {
            return extension;
        }

        String storedFileName = asset == null ? null : asset.getStoredFileName();
        extension = extractExtension(storedFileName == null ? "" : storedFileName);
        return extension.isBlank() ? "bin" : extension;
    }

    private WorkspaceAssetDto.AssetRes toAssetRes(WorkspaceAsset asset) {
        String downloadUrl = generatePresignedGetUrl(asset.getObjectKey());
        String previewUrl  = asset.getAssetType() == WorkspaceAssetType.IMAGE ? downloadUrl : null;

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
    /**
     * 이미지 파일 여부 판단
     */
    private boolean isImageFile(String contentType, String extension) {
        // ContentType으로 확인
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase(Locale.ROOT);
            if (lowerContentType.startsWith("image/")) {
                return true;
            }
        }

        // 확장자로 확인
        if (extension != null && !extension.isBlank()) {
            String lowerExtension = extension.toLowerCase(Locale.ROOT);
            return IMAGE_EXTENSIONS.contains(lowerExtension);
        }

        return false;
    }

    private String generatePresignedGetUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket_cloud())
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
                            .bucket(minioProperties.getBucket_cloud())
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

    private void publishAssetEvent(
            Long workspaceIdx,
            String action,
            Long actorUserIdx,
            List<WorkspaceAssetDto.AssetRes> assets,
            List<Long> assetIdxList
    ) {
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

    // ─── 버킷 이름 해석 ───────────────────────────────────────────────────────
    // 워크스페이스 에셋은 application.yml 의 minio.bucket_work 버킷에 저장합니다.
    // 드라이브 저장 시 복사 대상은 minio.bucket_cloud 버킷입니다.
    // FileUpDownloadMinioService 와 동일한 규칙을 따릅니다.

    private String resolveWorkspaceBucketName() {
        return minioProperties.getBucket_work();
    }

    private String resolveDriveBucketName() {
        return minioProperties.getBucket_cloud();
    }

    // ─── 기타 헬퍼 ────────────────────────────────────────────────────────────

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
}