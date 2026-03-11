package com.example.WaffleBear.file.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUpDownloadMinioService implements FileUpDownloadService {

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024 * 1024; // 5GB
    private static final long PARTITION_SIZE_BYTES = 100L * 1024 * 1024; // 100MB
    private static final long CHUNK_SIZE_BYTES = 80L * 1024 * 1024; // 80MB
    private static final long MIN_FINAL_PARTITION_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

//    @PostConstruct
//    public void ensureBucketExists() {
//        try {
//            String bucket = minioProperties.getBucket_cloud();
//            boolean exists = minioClient.bucketExists(
//                    BucketExistsArgs.builder()
//                            .bucket(bucket)
//                            .build()
//            );
//            if (!exists) {
//                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException("MinIO bucket init failed: " + minioProperties.getBucket_cloud(), e);
//        }
//    }

    @Override
    public List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests) {
        if (requests == null || requests.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        if (requests.size() > 1000) {
            throw BaseException.from(BaseResponseStatus.FILE_COUNT_WRONG);
        }

        List<FileInfoDto.FileRes> result = new ArrayList<>();

        for (FileInfoDto.FileReq req : requests) {
            validate(req);

            String fileOriginName = req.getFileOriginName().trim();
            String fileFormat = normalizeFormat(req.getFileFormat(), fileOriginName);
            Long userIdx = resolveUserIdx();
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
        saveFinalFileInfo(fileOriginName, fileFormat, fileSaveName, finalObjectKey, request.getFileSize(), userIdx);
        cleanupPartitionObjects(chunkObjectKeys);

        return FileInfoDto.CompleteRes.builder()
                .fileOriginName(fileOriginName)
                .fileSaveName(fileSaveName)
                .fileFormat(fileFormat)
                .finalObjectKey(finalObjectKey)
                .build();
    }

    private Long resolveUserIdx() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userIdx = 0L;
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails) {
            AuthUserDetails userDetails = (AuthUserDetails) authentication.getPrincipal();
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

        saveFinalFileInfo(fileOriginName, fileFormat, fileSaveName, objectKey, req.getFileSize(), userIdx);

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

        // Avoid creating a tiny tail partition; include it in the previous chunk instead.
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
            Long userIdx) {

        FileInfo entity = FileInfo.builder()
                .fileOriginName(fileOriginName)
                .fileFormat(fileFormat)
                .fileSaveName(fileSaveName)
                .fileSavePath(fileSavePath)
                .fileSize(fileSize)
                .lockedFile(false)
                .sharedFile(false)
                .user(User.builder()
                        .idx(userIdx)
                        .build())
                .build();

        fileUpDownloadRepository.save(entity);
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

    private FileInfoDto.FileListItemRes toFileListItem(FileInfo entity) {
        return FileInfoDto.FileListItemRes.builder()
                .idx(entity.getIdx())
                .fileOriginName(entity.getFileOriginName())
                .fileSaveName(entity.getFileSaveName())
                .fileSavePath(entity.getFileSavePath())
                .fileFormat(entity.getFileFormat())
                .fileSize(entity.getFileSize())
                .lockedFile(entity.isLockedFile())
                .sharedFile(entity.isSharedFile())
                .uploadDate(entity.getUploadDate())
                .lastModifyDate(entity.getLastModifyDate())
                .presignedDownloadUrl(generatePresignedDownloadUrl(entity.getFileSavePath()))
                .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                .build();
    }

    private String generatePresignedDownloadUrl(String objectKey) {
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
}
