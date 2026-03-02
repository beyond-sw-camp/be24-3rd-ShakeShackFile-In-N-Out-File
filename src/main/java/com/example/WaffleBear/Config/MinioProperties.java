package com.example.WaffleBear.Config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@NoArgsConstructor
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket_cloud;
    private String bucket_work;
    private int presignedUrlExpirySeconds = 600;
}
