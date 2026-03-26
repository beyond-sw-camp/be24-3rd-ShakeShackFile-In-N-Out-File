package com.example.WaffleBear.file.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileNodeType;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileThumbnailService {

    private static final String THUMBNAIL_DIRECTORY_NAME = "thumbnails";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "heic", "avif", "apng", "jfif", "tif", "tiff"
    );
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mov", "avi", "mkv", "wmv", "webm", "m4v", "mpeg", "mpg", "ogv", "3gp"
    );

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final VideoThumbnailService videoThumbnailService;
    private final ImageThumbnailService imageThumbnailService;

    public ThumbnailPayload loadThumbnail(FileInfo file) {
        validateFile(file);

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String normalizedExtension = normalizeExtension(file.getFileFormat());
        if (VIDEO_EXTENSIONS.contains(normalizedExtension)) {
            return loadVideoThumbnail(file, objectKey);
        }
        if (IMAGE_EXTENSIONS.contains(normalizedExtension)) {
            return loadImageThumbnail(file, objectKey, normalizedExtension);
        }

        return null;
    }

    private ThumbnailPayload loadVideoThumbnail(FileInfo file, String objectKey) {
        String thumbnailObjectKey = buildThumbnailObjectKey(objectKey);
        if (!objectExists(thumbnailObjectKey)) {
            ensureVideoThumbnail(file, thumbnailObjectKey);
        }
        if (!objectExists(thumbnailObjectKey)) {
            return null;
        }

        return readObject(thumbnailObjectKey, "image/jpeg", file);
    }

    private ThumbnailPayload loadImageThumbnail(FileInfo file, String objectKey, String extension) {
        String thumbnailObjectKey = buildThumbnailObjectKey(objectKey);
        if (!objectExists(thumbnailObjectKey)) {
            ensureImageThumbnail(objectKey, thumbnailObjectKey);
        }
        if (objectExists(thumbnailObjectKey)) {
            return readObject(thumbnailObjectKey, "image/jpeg", file);
        }

        return readObject(objectKey, resolveImageContentType(extension), file);
    }

    private void ensureVideoThumbnail(FileInfo file, String thumbnailObjectKey) {
        if (!videoThumbnailService.supports(file.getFileFormat())) {
            return;
        }

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            byte[] thumbnailBytes = createVideoThumbnailBytes(file);
            uploadThumbnail(thumbnailObjectKey, thumbnailBytes);
        } catch (Exception ignored) {
        }
    }

    private byte[] createVideoThumbnailBytes(FileInfo file) throws Exception {
        String objectKey = file.getFileSavePath();
        java.nio.file.Path tempVideoPath = null;

        try {
            tempVideoPath = java.nio.file.Files.createTempFile("wafflebear-thumbnail-", "." + normalizeExtension(file.getFileFormat()));
            try (var objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build()
            )) {
                java.nio.file.Files.copy(objectStream, tempVideoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            return videoThumbnailService.createThumbnail(tempVideoPath);
        } finally {
            if (tempVideoPath != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempVideoPath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void ensureImageThumbnail(String objectKey, String thumbnailObjectKey) {
        try (var objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket_cloud())
                        .object(objectKey)
                        .build()
        )) {
            byte[] thumbnailBytes = imageThumbnailService.createThumbnail(objectStream);
            uploadThumbnail(thumbnailObjectKey, thumbnailBytes);
        } catch (Exception ignored) {
        }
    }

    private void uploadThumbnail(String thumbnailObjectKey, byte[] thumbnailBytes) throws Exception {
        if (thumbnailBytes == null || thumbnailBytes.length == 0 || thumbnailObjectKey == null || thumbnailObjectKey.isBlank()) {
            return;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(thumbnailBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(thumbnailObjectKey)
                            .stream(inputStream, thumbnailBytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );
        }
    }

    private ThumbnailPayload readObject(String objectKey, String contentType, FileInfo file) {
        try (var objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket_cloud())
                        .object(objectKey)
                        .build()
        );
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            objectStream.transferTo(outputStream);
            return new ThumbnailPayload(
                    outputStream.toByteArray(),
                    contentType,
                    buildEtag(file),
                    resolveLastModifiedEpochMillis(file)
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean objectExists(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void validateFile(FileInfo file) {
        if (file == null || resolveNodeType(file) != FileNodeType.FILE || file.isLockedFile() || file.isTrashed()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private FileNodeType resolveNodeType(FileInfo file) {
        return file.getNodeType() == null ? FileNodeType.FILE : file.getNodeType();
    }

    private String normalizeExtension(String extension) {
        return extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
    }

    private String buildThumbnailObjectKey(String objectKey) {
        int pathSeparatorIndex = objectKey.lastIndexOf('/');
        String directory = pathSeparatorIndex >= 0 ? objectKey.substring(0, pathSeparatorIndex + 1) : "";
        String fileName = pathSeparatorIndex >= 0 ? objectKey.substring(pathSeparatorIndex + 1) : objectKey;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;

        return directory + THUMBNAIL_DIRECTORY_NAME + "/" + baseName + ".jpg";
    }

    private String resolveImageContentType(String extension) {
        return switch (normalizeExtension(extension)) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            case "avif" -> "image/avif";
            case "apng" -> "image/apng";
            case "tif", "tiff" -> "image/tiff";
            default -> "image/jpeg";
        };
    }

    private String buildEtag(FileInfo file) {
        long modifiedAt = resolveLastModifiedEpochMillis(file);
        long size = file.getFileSize() == null ? 0L : file.getFileSize();
        return "thumb-" + file.getIdx() + "-" + modifiedAt + "-" + size;
    }

    private long resolveLastModifiedEpochMillis(FileInfo file) {
        if (file.getLastModifyDate() != null) {
            return file.getLastModifyDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (file.getUploadDate() != null) {
            return file.getUploadDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return 0L;
    }

    public record ThumbnailPayload(
            byte[] bytes,
            String contentType,
            String eTag,
            long lastModifiedEpochMillis
    ) {
    }
}
