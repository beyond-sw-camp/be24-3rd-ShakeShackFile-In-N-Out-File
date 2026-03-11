package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//@Service
//@Primary
@RequiredArgsConstructor
public class FileUpDownloadS3Service implements FileUpDownloadService {

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @PostConstruct
    public void initBucket() {
        String bucket = minioProperties.getBucket_cloud();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("S3 Presigned URL storage unavailable for bucket: " + bucket, e);
        }
    }

    @Override
    public List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Upload request is empty.");
        }

        List<FileInfoDto.FileRes> response = new ArrayList<>();
        for (FileInfoDto.FileReq req : requests) {
            validate(req);
            String saveName = UUID.randomUUID() + "." + normalizeFormat(req);

            FileInfo entity = FileInfo.builder()
                    .fileOriginName(req.getFileOriginName())
                    .fileSaveName(saveName)
                    .fileFormat(normalizeFormat(req))
                    .fileSize(req.getFileSize())
                    .lockedFile(false)
                    .sharedFile(false)
                    .build();
            FileInfo saved = fileUpDownloadRepository.save(entity);

            String presignedUploadUrl = generatePresignedUploadUrl(saveName);

            response.add(
                    FileInfoDto.FileRes.builder()
//                            .fileIdx(saved.getIdx())
                            .fileOriginName(saved.getFileOriginName())
                            .fileSaveName(saved.getFileSaveName())
                            .presignedUploadUrl(presignedUploadUrl)
//                            .objectUrl(trim(minioProperties.getEndpoint()) + "/" + minioProperties.getBucket_cloud() + "/" + saveName)
                            .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                            .build()
            );
        }

        return response;
    }

    @Override
    public FileInfoDto.CompleteRes completeUpload(FileInfoDto.CompleteReq request) {
        return null;
    }

    @Override
    public FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto) {
        throw new UnsupportedOperationException("S3 service currently supports presigned upload only.");
    }

    @Override
    public List<FileInfoDto.FileListItemRes> fileList(Long idx) {
        return List.of();
    }

    @Override
    public FileInfoDto.FileListItemRes createFolder(FileInfoDto.FolderReq request) {
        throw new UnsupportedOperationException("S3 service currently does not support folder operations.");
    }

    @Override
    public FileInfoDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support trash operations.");
    }

    @Override
    public FileInfoDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support permanent delete operations.");
    }

    @Override
    public FileInfoDto.FileActionRes clearTrash(Long userIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support trash operations.");
    }

    @Override
    public FileInfoDto.FileActionRes moveToFolder(Long userIdx, Long fileIdx, Long targetParentId) {
        throw new UnsupportedOperationException("S3 service currently does not support move operations.");
    }

    @Override
    public FileInfoDto.FileListItemRes renameFolder(Long userIdx, Long folderIdx, String folderName) {
        throw new UnsupportedOperationException("S3 service currently does not support rename operations.");
    }

    @Override
    public FileInfoDto.FolderPropertyRes getFolderProperties(Long userIdx, Long folderIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support folder property operations.");
    }

    @Override
    public FileInfoDto.FileActionRes moveFilesToFolder(Long userIdx, List<Long> fileIdxList, Long targetParentId) {
        throw new UnsupportedOperationException("S3 service currently does not support batch move operations.");
    }

    @Override
    public FileInfoDto.StorageSummaryRes getStorageSummary(Long userIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support storage summary operations.");
    }

    @Override
    public FileInfoDto.TextPreviewRes getTextPreview(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support text preview operations.");
    }

    private String normalizeFormat(FileInfoDto.FileReq req) {
        String format = req.getFileFormat();
            if (format != null && !format.isBlank()) {
                format = format.trim();
                if (format.startsWith(".")) {
                    format = format.substring(1);
                }
                if (format.length() > 0) {
                return format.toLowerCase();
            }
        }
        String origin = req.getFileOriginName();
        int idx = origin.lastIndexOf('.');
        if (idx < 0 || idx == origin.length() - 1) {
            throw new IllegalArgumentException("File format is missing.");
        }
        return origin.substring(idx + 1).toLowerCase();
    }

    private void validate(FileInfoDto.FileReq req) {
        if (req == null) {
            throw new IllegalArgumentException("Upload request item is required.");
        }
        if (req.getFileOriginName() == null || req.getFileOriginName().isBlank()) {
            throw new IllegalArgumentException("Origin file name is required.");
        }
        if (req.getFileSize() == null || req.getFileSize() <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero.");
        }
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String generatePresignedUploadUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectName)
                            .expiry(minioProperties.getPresignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate presigned upload url for object: " + objectName, e);
        }
    }
}
