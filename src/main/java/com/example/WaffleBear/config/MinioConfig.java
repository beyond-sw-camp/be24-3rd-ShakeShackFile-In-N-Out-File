package com.example.WaffleBear.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그 기록을 위해 추가
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {
    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        // 1. MinioClient 빌드
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey());

        if (StringUtils.hasText(minioProperties.getRegion())) {
            builder.region(minioProperties.getRegion());
        }

        MinioClient minioClient = builder.build();

        // 2. 버킷 자동 생성 로직 추가
        initBucket(minioClient);

        return minioClient;
    }

    // MinioConfig.java 내의 initBucket 부분 수정
    private void initBucket(MinioClient minioClient) {
        // 기존 메서드 명칭에 맞춰 호출
        String[] buckets = { minioProperties.getBucket_cloud(), minioProperties.getBucket_work() };

        for (String bucketName : buckets) {
            if (bucketName == null || bucketName.isBlank()) continue;

            try {
                boolean found = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucketName).build()
                );

                if (!found) {
                    log.info("MinIO 버킷 자동 생성 시도: [{}]", bucketName);
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(bucketName).build()
                    );
                }
            } catch (Exception e) {
                log.error("[{}] 버킷 초기화 실패: {}", bucketName, e.getMessage());
            }
        }
    }
}