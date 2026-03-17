package com.example.WaffleBear.feater;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.feater.model.Feater;
import com.example.WaffleBear.feater.model.FeaterDto;
import com.example.WaffleBear.file.service.StoragePlanService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeaterService {

    private static final int PROFILE_IMAGE_SIZE = 300;
    private static final int PROFILE_IMAGE_MAX_DIMENSION = 4096;
    private static final long PROFILE_IMAGE_MAX_SOURCE_BYTES = 50L * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final String PROFILE_IMAGE_DIRECTORY = "userProfileImage";
    private static final String PROFILE_IMAGE_FILE_NAME = "profile.png";

    private final FeaterRepository featerRepository;
    private final UserRepository userRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final StoragePlanService storagePlanService;

    public FeaterDto.SettingsRes getSettings(Long userIdx) {
        User user = getUser(userIdx);
        Feater settings = getOrCreateSettings(user);
        return toResponse(user, settings);
    }

    public FeaterDto.SettingsRes updateSettings(Long userIdx, FeaterDto.SettingsUpdateReq request) {
        if (request == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        User user = getUser(userIdx);
        Feater settings = getOrCreateSettings(user);

        String displayName = normalizeDisplayName(request.getDisplayName());

        settings.update(
                normalizeDisplayName(request.getDisplayName()),
                normalizeLocaleCode(request.getLocaleCode()),
                normalizeRegionCode(request.getRegionCode()),
                request.getMarketingOptIn() != null ? request.getMarketingOptIn() : Boolean.TRUE,
                request.getPrivateProfile() != null ? request.getPrivateProfile() : Boolean.FALSE,
                request.getEmailNotification() != null ? request.getEmailNotification() : Boolean.TRUE,
                request.getSecurityNotification() != null ? request.getSecurityNotification() : Boolean.TRUE,
                settings.getProfileImageUrl()
        );

        user.setName(displayName);
        userRepository.save(user);

        Feater saved = featerRepository.save(settings);
        return toResponse(user, saved);
    }

    public FeaterDto.SettingsRes uploadProfileImage(Long userIdx, MultipartFile image) {
        validateProfileImage(image);

        User user = getUser(userIdx);
        Feater settings = getOrCreateSettings(user);

        BufferedImage sourceImage = readImage(image);
        BufferedImage normalizedImage = downscaleIfNeeded(sourceImage, PROFILE_IMAGE_MAX_DIMENSION);
        BufferedImage resizedImage = resizeToSquare(normalizedImage, PROFILE_IMAGE_SIZE);
        String objectKey = buildProfileImageObjectKey(user.getIdx());

        uploadProfileImageToMinio(resizedImage, objectKey);
        settings.updateProfileImage(objectKey);
        Feater saved = featerRepository.save(settings);

        return toResponse(user, saved);
    }

    private User getUser(Long userIdx) {
        if (userIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return userRepository.findById(userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
    }

    private Feater getOrCreateSettings(User user) {
        return featerRepository.findByUser_Idx(user.getIdx())
                .orElseGet(() -> featerRepository.save(
                        Feater.builder()
                                .user(user)
                                .displayName(resolveInitialDisplayName(user))
                                .localeCode("KO")
                                .regionCode("KR")
                                .marketingOptIn(true)
                                .privateProfile(false)
                                .emailNotification(true)
                                .securityNotification(true)
                                .profileImageUrl(null)
                                .build()
                ));
    }

    private FeaterDto.SettingsRes toResponse(User user, Feater settings) {
        StoragePlanService.StorageQuota storageQuota = storagePlanService.resolveQuota(user);

        return FeaterDto.SettingsRes.builder()
                .userIdx(user.getIdx())
                .email(user.getEmail())
                .displayName(settings.getDisplayName())
                .role(user.getRole())
                .emailVerified(Boolean.TRUE.equals(user.getEnable()))
                .localeCode(settings.getLocaleCode())
                .regionCode(settings.getRegionCode())
                .marketingOptIn(Boolean.TRUE.equals(settings.getMarketingOptIn()))
                .privateProfile(Boolean.TRUE.equals(settings.getPrivateProfile()))
                .emailNotification(Boolean.TRUE.equals(settings.getEmailNotification()))
                .securityNotification(Boolean.TRUE.equals(settings.getSecurityNotification()))
                .profileImageUrl(resolveProfileImagePreview(settings.getProfileImageUrl()))
                .membershipCode(storageQuota.planCode())
                .membershipLabel(storageQuota.membershipLabel())
                .storagePlanLabel(storageQuota.planLabel())
                .storageQuotaBytes(storageQuota.totalQuotaBytes())
                .storageBaseQuotaBytes(storageQuota.baseQuotaBytes())
                .storageAddonBytes(storageQuota.addonQuotaBytes())
                .joinedAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private String normalizeDisplayName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return normalized;
    }

    private String resolveInitialDisplayName(User user) {
        String candidate = user.getName();
        if (candidate != null && !candidate.isBlank() && candidate.trim().length() <= 100) {
            return candidate.trim();
        }

        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf("@")).trim();
            if (!localPart.isBlank() && localPart.length() <= 100) {
                return localPart;
            }
        }

        return "User";
    }

    private String normalizeLocaleCode(String value) {
        String normalized = value == null ? "KO" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 10) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return normalized;
    }

    private String normalizeRegionCode(String value) {
        String normalized = value == null ? "KR" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 10) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return normalized;
    }

    private void validateProfileImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        if (image.getSize() > PROFILE_IMAGE_MAX_SOURCE_BYTES) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private BufferedImage readImage(MultipartFile image) {
        try {
            BufferedImage sourceImage = ImageIO.read(image.getInputStream());
            if (sourceImage == null) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
            return sourceImage;
        } catch (IOException exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private BufferedImage downscaleIfNeeded(BufferedImage sourceImage, int maxDimension) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int safeMaxDimension = Math.max(PROFILE_IMAGE_SIZE, maxDimension);

        if (width <= safeMaxDimension && height <= safeMaxDimension) {
            return sourceImage;
        }

        double scale = Math.min(
                safeMaxDimension / (double) width,
                safeMaxDimension / (double) height
        );

        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage resizedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(sourceImage, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();
        return resizedImage;
    }

    private BufferedImage resizeToSquare(BufferedImage sourceImage, int targetSize) {
        double scale = Math.max(
                targetSize / (double) sourceImage.getWidth(),
                targetSize / (double) sourceImage.getHeight()
        );

        int scaledWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));
        int x = (targetSize - scaledWidth) / 2;
        int y = (targetSize - scaledHeight) / 2;

        BufferedImage resizedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(sourceImage, x, y, scaledWidth, scaledHeight, null);
        graphics.dispose();

        return resizedImage;
    }

    private void uploadProfileImageToMinio(BufferedImage resizedImage, String objectKey) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(resizedImage, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucket_cloud())
                                .object(objectKey)
                                .stream(inputStream, imageBytes.length, -1)
                                .contentType("image/png")
                                .build()
                );
            }
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private String buildProfileImageObjectKey(Long userIdx) {
        if (userIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return PROFILE_IMAGE_DIRECTORY + "/" + userIdx + "/" + PROFILE_IMAGE_FILE_NAME;
    }

    private String resolveProfileImagePreview(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }

        if (storedValue.startsWith("data:image") || storedValue.startsWith("http://") || storedValue.startsWith("https://")) {
            return storedValue;
        }

        if (storedValue.startsWith(PROFILE_IMAGE_DIRECTORY + "/")) {
            try {
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(minioProperties.getBucket_cloud())
                                .object(storedValue)
                                .expiry(minioProperties.getPresignedUrlExpirySeconds())
                                .build()
                );
            } catch (Exception exception) {
                return null;
            }
        }

        if (isLegacyStoredProfileImageFileName(storedValue)) {
            return resolveLegacyProfileImagePreview(storedValue);
        }

        return null;
    }

    private String resolveLegacyProfileImagePreview(String storedValue) {
        Path targetPath = Paths.get("src", "main", "resources", "upload", "userImage")
                .toAbsolutePath()
                .normalize()
                .resolve(storedValue)
                .normalize();

        if (!Files.exists(targetPath)) {
            return null;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(targetPath);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException exception) {
            return null;
        }
    }

    private boolean isLegacyStoredProfileImageFileName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        if (value.contains("/") || value.contains("\\") || value.contains(":")) {
            return false;
        }

        return value.matches("[A-Za-z0-9._-]+");
    }

    // 채팅에서 쓸 프로필사진
    public String resolveProfileImage(Long userIdx) {
        return featerRepository.findByUser_Idx(userIdx)
                .map(f -> resolveProfileImagePreview(f.getProfileImageUrl()))
                .orElse(null);
    }
}