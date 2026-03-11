package com.example.WaffleBear.file.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUpDownloadMinioService implements FileUpDownloadService {

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024 * 1024;
    private static final long PARTITION_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long CHUNK_SIZE_BYTES = 80L * 1024 * 1024;
    private static final long MIN_FINAL_PARTITION_SIZE_BYTES = 10L * 1024 * 1024;

    @Override
    public List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests) {
        if (requests == null || requests.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        if (requests.size() > 1000) {
            throw BaseException.from(BaseResponseStatus.FILE_COUNT_WRONG);
        }

        Long userIdx = resolveUserIdx();
        List<FileInfoDto.FileRes> result = new ArrayList<>();

        for (FileInfoDto.FileReq req : requests) {
            validate(req);

            String fileOriginName = req.getFileOriginName().trim();
            String fileFormat = normalizeFormat(req.getFileFormat(), fileOriginName);
            String basicPath = userIdx + "/";

            if (shouldPartition(req.getFileSize())) {
                result.addAll(createPartitionResponses(req, fileOriginName, fileFormat, basicPath));
            } else {
                result.add(createSingleResponse(req, fileOriginName, fileFormat, basicPath, userIdx));
            }
        }

        return result;
    }

    @Override
    public FileInfoDto.CompleteRes completeUpload(FileInfoDto.CompleteReq request) {
        validateCompleteRequest(request);

        String fileOriginName = request.getFileOriginName().trim();
        String fileFormat = normalizeFormat(request.getFileFormat(), fileOriginName);
        String finalObjectKey = request.getFinalObjectKey();
        List<String> chunkObjectKeys = request.getChunkObjectKeys();
        String fileSaveName = extractObjectName(finalObjectKey);
        Long userIdx = resolveUserIdx();

        composeFinalObject(finalObjectKey, chunkObjectKeys);
        saveFinalFileInfo(
                fileOriginName,
                fileFormat,
                fileSaveName,
                finalObjectKey,
                request.getFileSize(),
                userIdx,
                request.getParentId()
        );
        cleanupPartitionObjects(chunkObjectKeys);

        return FileInfoDto.CompleteRes.builder()
                .fileOriginName(fileOriginName)
                .fileSaveName(fileSaveName)
                .fileFormat(fileFormat)
                .finalObjectKey(finalObjectKey)
                .build();
    }

    @Override
    public FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto) {
        return null;
    }

    @Override
    public List<FileInfoDto.FileListItemRes> fileList(Long idx) {
        Long userIdx = idx == null ? 0L : idx;

        return fileUpDownloadRepository.findAllByUser_IdxOrderByLastModifyDateDescUploadDateDesc(userIdx)
                .stream()
                .map(this::toFileListItem)
                .toList();
    }

    @Override
    public FileInfoDto.FileListItemRes createFolder(FileInfoDto.FolderReq request) {
        if (request == null || request.getFolderName() == null || request.getFolderName().isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        Long userIdx = resolveUserIdx();
        String folderName = sanitizeFolderName(request.getFolderName());
        FileInfo parent = resolveParentFolder(userIdx, request.getParentId());

        FileInfo entity = FileInfo.builder()
                .user(User.builder().idx(userIdx).build())
                .parent(parent)
                .nodeType(FileNodeType.FOLDER)
                .fileOriginName(folderName)
                .fileFormat("folder")
                .fileSaveName("folder-" + UUID.randomUUID())
                .fileSavePath(null)
                .fileSize(0L)
                .lockedFile(false)
                .sharedFile(false)
                .trashed(false)
                .deletedAt(null)
                .build();

        return toFileListItem(fileUpDownloadRepository.save(entity));
    }

    @Override
    public FileInfoDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx) {
        FileInfo target = getOwnedFile(userIdx, fileIdx);
        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        List<FileInfo> targetTree = collectTargetTree(target, userFiles);
        LocalDateTime deletedAt = LocalDateTime.now();

        targetTree.forEach(file -> file.markTrashed(deletedAt));
        fileUpDownloadRepository.saveAll(targetTree);

        return FileInfoDto.FileActionRes.builder()
                .targetIdx(fileIdx)
                .action("trash")
                .affectedCount(targetTree.size())
                .build();
    }

    @Override
    public FileInfoDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx) {
        FileInfo target = getOwnedFile(userIdx, fileIdx);
        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        List<FileInfo> targetTree = collectTargetTree(target, userFiles);

        removeMinioObjects(targetTree);
        fileUpDownloadRepository.deleteAll(sortForDelete(targetTree));

        return FileInfoDto.FileActionRes.builder()
                .targetIdx(fileIdx)
                .action("permanent-delete")
                .affectedCount(targetTree.size())
                .build();
    }

    @Override
    public FileInfoDto.FileActionRes clearTrash(Long userIdx) {
        List<FileInfo> trashedFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx)
                .stream()
                .filter(FileInfo::isTrashed)
                .toList();

        if (trashedFiles.isEmpty()) {
            return FileInfoDto.FileActionRes.builder()
                    .targetIdx(null)
                    .action("clear-trash")
                    .affectedCount(0)
                    .build();
        }

        removeMinioObjects(trashedFiles);
        fileUpDownloadRepository.deleteAll(sortForDelete(trashedFiles));

        return FileInfoDto.FileActionRes.builder()
                .targetIdx(null)
                .action("clear-trash")
                .affectedCount(trashedFiles.size())
                .build();
    }

    private Long resolveUserIdx() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userIdx = 0L;
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            userIdx = userDetails.getIdx();
        }
        return userIdx;
    }

    private boolean shouldPartition(Long fileSize) {
        return fileSize != null && fileSize > PARTITION_SIZE_BYTES;
    }

    private List<FileInfoDto.FileRes> createPartitionResponses(
            FileInfoDto.FileReq req,
            String fileOriginName,
            String fileFormat,
            String basicPath) {

        List<FileInfoDto.FileRes> responses = new ArrayList<>();
        String finalSaveName = UUID.randomUUID() + "." + fileFormat;
        String finalObjectKey = basicPath + finalSaveName;
        String partitionBase = UUID.randomUUID().toString();
        int partitionCount = calculatePartitionCount(req.getFileSize());

        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            String objectKey = buildPartitionObjectKey(basicPath, partitionBase, fileFormat, partitionIndex, partitionCount);
            responses.add(FileInfoDto.FileRes.builder()
                    .fileOriginName(fileOriginName)
                    .fileSaveName(finalSaveName)
                    .fileFormat(fileFormat)
                    .presignedUploadUrl(generatePresignedUploadUrl(objectKey))
                    .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                    .objectKey(objectKey)
                    .finalObjectKey(finalObjectKey)
                    .partitionIndex(partitionIndex + 1)
                    .partitionCount(partitionCount)
                    .partitioned(true)
                    .build());
        }

        return responses;
    }

    private FileInfoDto.FileRes createSingleResponse(
            FileInfoDto.FileReq req,
            String fileOriginName,
            String fileFormat,
            String basicPath,
            Long userIdx) {

        String fileSaveName = UUID.randomUUID() + "." + fileFormat;
        String objectKey = basicPath + fileSaveName;

        saveFinalFileInfo(
                fileOriginName,
                fileFormat,
                fileSaveName,
                objectKey,
                req.getFileSize(),
                userIdx,
                req.getParentId()
        );

        return FileInfoDto.FileRes.builder()
                .fileOriginName(fileOriginName)
                .fileSaveName(fileSaveName)
                .fileFormat(fileFormat)
                .presignedUploadUrl(generatePresignedUploadUrl(objectKey))
                .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                .objectKey(objectKey)
                .finalObjectKey(objectKey)
                .partitionIndex(1)
                .partitionCount(1)
                .partitioned(false)
                .build();
    }

    private int calculatePartitionCount(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            return 1;
        }

        long partitionCount = (fileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES;
        long remainder = fileSize % CHUNK_SIZE_BYTES;

        if (partitionCount > 1 && remainder > 0 && remainder <= MIN_FINAL_PARTITION_SIZE_BYTES) {
            return (int) (partitionCount - 1);
        }

        return (int) partitionCount;
    }

    private String buildPartitionObjectKey(
            String basicPath,
            String partitionBase,
            String fileFormat,
            int partitionIndex,
            int partitionCount) {

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

    private void saveFinalFileInfo(
            String fileOriginName,
            String fileFormat,
            String fileSaveName,
            String fileSavePath,
            Long fileSize,
            Long userIdx,
            Long parentId) {

        FileInfo entity = FileInfo.builder()
                .fileOriginName(fileOriginName)
                .fileFormat(fileFormat)
                .fileSaveName(fileSaveName)
                .fileSavePath(fileSavePath)
                .fileSize(fileSize)
                .nodeType(FileNodeType.FILE)
                .parent(resolveParentFolder(userIdx, parentId))
                .lockedFile(false)
                .sharedFile(false)
                .trashed(false)
                .deletedAt(null)
                .user(User.builder().idx(userIdx).build())
                .build();

        fileUpDownloadRepository.save(entity);
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

    private FileInfo getOwnedFile(Long userIdx, Long fileIdx) {
        if (userIdx == null || fileIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return fileUpDownloadRepository.findByIdxAndUser_Idx(fileIdx, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
    }

    private List<FileInfo> collectTargetTree(FileInfo target, List<FileInfo> userFiles) {
        Map<Long, List<FileInfo>> childrenByParent = new HashMap<>();
        for (FileInfo file : userFiles) {
            Long parentId = file.getParent() != null ? file.getParent().getIdx() : null;
            if (parentId == null) {
                continue;
            }
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(file);
        }

        List<FileInfo> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<FileInfo> queue = new ArrayDeque<>();
        queue.add(target);

        while (!queue.isEmpty()) {
            FileInfo current = queue.removeFirst();
            if (current.getIdx() == null || !visited.add(current.getIdx())) {
                continue;
            }

            result.add(current);
            for (FileInfo child : childrenByParent.getOrDefault(current.getIdx(), List.of())) {
                queue.addLast(child);
            }
        }

        return result;
    }

    private List<FileInfo> sortForDelete(List<FileInfo> targetTree) {
        Map<Long, FileInfo> fileById = targetTree.stream()
                .filter(file -> file.getIdx() != null)
                .collect(HashMap::new, (map, file) -> map.put(file.getIdx(), file), HashMap::putAll);

        return targetTree.stream()
                .sorted(Comparator.comparingInt((FileInfo file) -> calculateDepth(file, fileById)).reversed())
                .toList();
    }

    private int calculateDepth(FileInfo file, Map<Long, FileInfo> fileById) {
        int depth = 0;
        Set<Long> visited = new HashSet<>();
        FileInfo current = file.getParent();

        while (current != null && current.getIdx() != null && visited.add(current.getIdx())) {
            depth += 1;
            FileInfo resolved = fileById.get(current.getIdx());
            current = resolved != null ? resolved.getParent() : current.getParent();
        }

        return depth;
    }

    private void removeMinioObjects(List<FileInfo> files) {
        List<DeleteObject> deleteTargets = files.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .map(FileInfo::getFileSavePath)
                .filter(path -> path != null && !path.isBlank())
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
        } catch (Exception ignored) {
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

    private void cleanupPartitionObjects(List<String> chunkObjectKeys) {
        try {
            List<DeleteObject> objects = chunkObjectKeys.stream()
                    .map(DeleteObject::new)
                    .toList();

            Iterable<Result<io.minio.messages.DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .objects(objects)
                            .build()
            );

            for (Result<io.minio.messages.DeleteError> result : results) {
                result.get();
            }
        } catch (Exception ignored) {
        }
    }

    private String generatePresignedUploadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build());
        } catch (Exception e) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    private String generatePresignedDownloadUrl(FileInfo entity) {
        if (resolveNodeType(entity) != FileNodeType.FILE) {
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
        } catch (Exception e) {
            return null;
        }
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

    private String sanitizeFolderName(String folderName) {
        String normalized = folderName == null ? "" : folderName.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\") || normalized.contains("\u0000")) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        return normalized;
    }

    private void validate(FileInfoDto.FileReq req) {
        if (req == null) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        String originName = req.getFileOriginName();
        if (originName == null || originName.isBlank()) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        if (originName.length() > 100) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_LENGTH_WRONG);
        }

        if (originName.contains("..") || originName.contains("/") || originName.contains("\\") || originName.contains("\u0000")) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        Long fileSize = req.getFileSize();
        if (fileSize != null && fileSize > MAX_SIZE_BYTES) {
            throw BaseException.from(BaseResponseStatus.FILE_SIZE_WRONG);
        }
    }

    private void validateCompleteRequest(FileInfoDto.CompleteReq request) {
        if (request == null) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        if (request.getFileOriginName() == null || request.getFileOriginName().isBlank()) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        if (request.getFinalObjectKey() == null || request.getFinalObjectKey().isBlank()) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }

        if (request.getChunkObjectKeys() == null || request.getChunkObjectKeys().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    private String extractObjectName(String objectKey) {
        int idx = objectKey.lastIndexOf('/');
        if (idx < 0 || idx == objectKey.length() - 1) {
            return objectKey;
        }
        return objectKey.substring(idx + 1);
    }

    private FileInfoDto.FileListItemRes toFileListItem(FileInfo entity) {
        FileNodeType nodeType = resolveNodeType(entity);

        return FileInfoDto.FileListItemRes.builder()
                .idx(entity.getIdx())
                .fileOriginName(entity.getFileOriginName())
                .fileSaveName(entity.getFileSaveName())
                .fileSavePath(entity.getFileSavePath())
                .fileFormat(entity.getFileFormat())
                .fileSize(entity.getFileSize())
                .nodeType(nodeType.name())
                .parentId(entity.getParent() != null ? entity.getParent().getIdx() : null)
                .lockedFile(entity.isLockedFile())
                .sharedFile(entity.isSharedFile())
                .trashed(entity.isTrashed())
                .deletedAt(entity.getDeletedAt())
                .uploadDate(entity.getUploadDate())
                .lastModifyDate(entity.getLastModifyDate())
                .presignedDownloadUrl(generatePresignedDownloadUrl(entity))
                .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                .build();
    }

    private FileNodeType resolveNodeType(FileInfo entity) {
        return entity.getNodeType() == null ? FileNodeType.FILE : entity.getNodeType();
    }
}
