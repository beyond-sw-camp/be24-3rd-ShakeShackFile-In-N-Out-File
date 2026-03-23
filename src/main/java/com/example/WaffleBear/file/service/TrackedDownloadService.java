package com.example.WaffleBear.file.service;

import com.example.WaffleBear.administrator.StorageAnalyticsService;
import com.example.WaffleBear.administrator.storage.DataTransferSource;
import com.example.WaffleBear.administrator.storage.DataTransferStatus;
import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TrackedDownloadService {

    private final MinioClient minioClient;
    private final StorageAnalyticsService storageAnalyticsService;

    public ResponseEntity<StreamingResponseBody> streamObject(
            Long userIdx,
            String bucketName,
            String objectKey,
            String fileName,
            String fallbackContentType,
            DataTransferSource source,
            String referenceLabel
    ) {
        if (bucketName == null || bucketName.isBlank() || objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        StatObjectResponse objectStat = statObject(bucketName, objectKey);
        String contentType = resolveContentType(objectStat.contentType(), fallbackContentType);
        String downloadFileName = resolveFileName(fileName, objectKey);

        StreamingResponseBody body = outputStream -> {
            long transferredBytes = 0L;
            DataTransferStatus transferStatus = DataTransferStatus.COMPLETED;

            try (InputStream objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            )) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = objectStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    transferredBytes += read;
                }
                outputStream.flush();
            } catch (IOException exception) {
                transferStatus = transferredBytes > 0L ? DataTransferStatus.PARTIAL : DataTransferStatus.FAILED;
                throw exception;
            } catch (Exception exception) {
                transferStatus = transferredBytes > 0L ? DataTransferStatus.PARTIAL : DataTransferStatus.FAILED;
                throw new IOException(exception);
            } finally {
                if (transferredBytes > 0L) {
                    storageAnalyticsService.recordEgress(
                            userIdx,
                            source,
                            transferStatus,
                            transferredBytes,
                            objectKey,
                            referenceLabel
                    );
                }
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(downloadFileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentLength(Math.max(0L, objectStat.size()))
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    public long statObjectSize(String bucketName, String objectKey) {
        return Math.max(0L, statObject(bucketName, objectKey).size());
    }

    private StatObjectResponse statObject(String bucketName, String objectKey) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private String resolveContentType(String primaryContentType, String fallbackContentType) {
        if (primaryContentType != null && !primaryContentType.isBlank()) {
            return primaryContentType;
        }
        if (fallbackContentType != null && !fallbackContentType.isBlank()) {
            return fallbackContentType;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String resolveFileName(String fileName, String objectKey) {
        if (fileName != null && !fileName.isBlank()) {
            return fileName.trim();
        }

        int separatorIndex = objectKey.lastIndexOf('/');
        return separatorIndex >= 0 && separatorIndex < objectKey.length() - 1
                ? objectKey.substring(separatorIndex + 1)
                : objectKey;
    }
}
