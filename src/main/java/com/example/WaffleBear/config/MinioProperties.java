package com.example.WaffleBear.config;

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "minio")
@Setter
@NoArgsConstructor
public class MinioProperties {
    private static final String DEFAULT_PROVIDER = "minio";

    private String provider;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket_cloud;
    private String bucket_work;
    private String region;

    private String s3Endpoint;
    private String s3AccessKey;
    private String s3SecretKey;
    private String s3BucketCloud;
    private String s3BucketWork;
    private String s3Region;

    private int presignedUrlExpirySeconds = 6000;

    public String getProvider() {
        return normalizeProvider(provider);
    }

    public String getEndpoint() {
        if (isS3Provider()) {
            return firstNonBlank(s3Endpoint, endpoint);
        }
        return endpoint;
    }

    public String getAccessKey() {
        if (isS3Provider()) {
            return firstNonBlank(s3AccessKey, accessKey);
        }
        return accessKey;
    }

    public String getSecretKey() {
        if (isS3Provider()) {
            return firstNonBlank(s3SecretKey, secretKey);
        }
        return secretKey;
    }

    public String getBucket_cloud() {
        if (isS3Provider()) {
            return firstNonBlank(s3BucketCloud, bucket_cloud);
        }
        return bucket_cloud;
    }

    public String getBucket_work() {
        if (isS3Provider()) {
            return firstNonBlank(s3BucketWork, s3BucketCloud, bucket_work, bucket_cloud);
        }
        return firstNonBlank(bucket_work, bucket_cloud);
    }

    public String getRegion() {
        if (isS3Provider()) {
            return firstNonBlank(s3Region, region);
        }
        return region;
    }

    public int getPresignedUrlExpirySeconds() {
        return presignedUrlExpirySeconds;
    }

    public boolean isS3Provider() {
        return "s3".equals(getProvider());
    }

    private String normalizeProvider(String rawProvider) {
        if (rawProvider == null || rawProvider.isBlank()) {
            return DEFAULT_PROVIDER;
        }
        return rawProvider.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }
}
