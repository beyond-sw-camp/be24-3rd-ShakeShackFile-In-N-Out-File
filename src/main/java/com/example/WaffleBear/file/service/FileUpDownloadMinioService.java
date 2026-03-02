package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.Config.MinioProperties;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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

    @PostConstruct
    public void ensureBucketExists() {
        try {
            String bucket = minioProperties.getBucket_cloud();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket: " + minioProperties.getBucket_cloud(), e);
        }
    }

    @Override
    public List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("No file metadata found.");
        }

        List<FileInfoDto.FileRes> result = new ArrayList<>();

        for (FileInfoDto.FileReq req : requests) {
            validate(req);
            String format = normalizeFormat(req);
            String originName = req.getFileOriginName().trim();
            String saveName = UUID.randomUUID() + "." + format;

            FileInfo entity = FileInfo.builder()
                    .fileOriginName(originName)
                    .fileSaveName(saveName)
                    .fileFormat(format)
                    .fileSize(req.getFileSize())
                    .lockedFile(false)
                    .sharedFile(false)
                    .build();

            FileInfo saved = fileUpDownloadRepository.save(entity);
            String uploadUrl = generatePresignedUploadUrl(saveName);

            FileInfoDto.FileRes res = FileInfoDto.FileRes.builder()
                    .fileIdx(saved.getIdx())
                    .fileOriginName(originName)
                    .fileSaveName(saveName)
                    .presignedUploadUrl(uploadUrl)
                    .objectUrl(buildObjectUrl(saveName))
                    .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                    .build();

            result.add(res);
        }

        return result;
    }

    private String generatePresignedUploadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectName)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create presigned upload URL for object: " + objectName, e);
        }
    }

    private String buildObjectUrl(String objectName) {
        String endpoint = trimTrailingSlash(minioProperties.getEndpoint());
        return endpoint + "/" + minioProperties.getBucket_cloud() + "/" + objectName;
    }

    private String normalizeFormat(FileInfoDto.FileReq req) {
        String format = req.getFileFormat();
        String originName = req.getFileOriginName();

        if (format == null || format.isBlank()) {
            int idx = originName.lastIndexOf('.');
            if (idx <= 0 || idx >= originName.length() - 1) {
                throw new IllegalArgumentException("File extension is required.");
            }
            format = originName.substring(idx + 1);
        }

        format = format.trim();
        if (format.startsWith(".")) {
            format = format.substring(1);
        }

        if (format.isEmpty() || format.length() > 20 || !format.matches("^[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("Invalid file extension.");
        }

        return format.toLowerCase();
    }

    private void validate(FileInfoDto.FileReq req) {
        if (req == null) {
            throw new IllegalArgumentException("File metadata is required.");
        }

        String originName = req.getFileOriginName();
        if (originName == null || originName.isBlank()) {
            throw new IllegalArgumentException("Origin file name is required.");
        }
        if (originName.length() > 255) {
            throw new IllegalArgumentException("Origin file name is too long.");
        }
        if (originName.contains("..") || originName.contains("/") || originName.contains("\\") || originName.contains("\u0000")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        if (req.getFileSize() == null || req.getFileSize() <= 0) {
            throw new IllegalArgumentException("File size must be greater than 0.");
        }
        if (req.getFileSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5GB.");
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    @Override
    public FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto) {
        return null;
    }

    @Override
    public FileInfoDto.FileRes fileList(Long idx) {
        return null;
    }
}
