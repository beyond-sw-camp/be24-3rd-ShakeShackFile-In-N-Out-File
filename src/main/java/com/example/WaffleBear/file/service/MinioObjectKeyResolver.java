package com.example.WaffleBear.file.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class MinioObjectKeyResolver {

    public String resolveObjectKey(String rawValue, String bucketName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim();
        if (normalized.isBlank()) {
            return null;
        }

        if (!normalized.contains("://") && !normalized.startsWith("/")) {
            return normalized;
        }

        try {
            URI uri = URI.create(normalized);
            String objectKeyFromQuery = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("objectKey");
            if (objectKeyFromQuery != null && !objectKeyFromQuery.isBlank()) {
                return URLDecoder.decode(objectKeyFromQuery, StandardCharsets.UTF_8);
            }

            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            String bucketPrefix = bucketName == null || bucketName.isBlank()
                    ? ""
                    : bucketName.trim() + "/";

            if (!bucketPrefix.isEmpty() && normalizedPath.startsWith(bucketPrefix)) {
                return URLDecoder.decode(normalizedPath.substring(bucketPrefix.length()), StandardCharsets.UTF_8);
            }

            return null;
        } catch (Exception exception) {
            return null;
        }
    }
}
