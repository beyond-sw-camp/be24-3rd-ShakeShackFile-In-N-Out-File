package com.example.WaffleBear.feater;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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

    private static final long BASIC_STORAGE_BYTES = 20L * 1024 * 1024 * 1024;
    private static final long PLUS_STORAGE_BYTES = 100L * 1024 * 1024 * 1024;
    private static final long PREMIUM_STORAGE_BYTES = 200L * 1024 * 1024 * 1024;
    private static final int PROFILE_IMAGE_SIZE = 300;
    private static final long PROFILE_IMAGE_MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");

    private final FeaterRepository featerRepository;
    private final UserRepository userRepository;

    public FeaterDto.SettingsRes getSettings(Long userIdx) {
        User user = getUser(userIdx);
        FeaterSettings settings = getOrCreateSettings(user);

        return toResponse(user, settings);
    }

    public FeaterDto.SettingsRes updateSettings(Long userIdx, FeaterDto.SettingsUpdateReq request) {
        if (request == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        User user = getUser(userIdx);
        FeaterSettings settings = getOrCreateSettings(user);

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

        FeaterSettings saved = featerRepository.save(settings);
        return toResponse(user, saved);
    }

    public FeaterDto.SettingsRes uploadProfileImage(Long userIdx, MultipartFile image) {
        validateProfileImage(image);

        User user = getUser(userIdx);
        FeaterSettings settings = getOrCreateSettings(user);

        BufferedImage sourceImage = readImage(image);
        BufferedImage resizedImage = resizeToSquare(sourceImage, PROFILE_IMAGE_SIZE);

        String fileName = "user-" + user.getIdx() + "-" + System.currentTimeMillis() + ".png";
        Path directory = resolveProfileImageDirectory();
        Path targetPath = directory.resolve(fileName).normalize();

        try {
            Files.createDirectories(directory);
            deleteStoredProfileImage(settings.getProfileImageUrl());
            ImageIO.write(resizedImage, "png", targetPath.toFile());
        } catch (IOException exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        settings.updateProfileImage(fileName);
        FeaterSettings saved = featerRepository.save(settings);

        return toResponse(user, saved);
    }

    private User getUser(Long userIdx) {
        if (userIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return userRepository.findById(userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
    }

    private FeaterSettings getOrCreateSettings(User user) {
        return featerRepository.findByUser_Idx(user.getIdx())
                .orElseGet(() -> featerRepository.save(
                        FeaterSettings.builder()
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

    private FeaterDto.SettingsRes toResponse(User user, FeaterSettings settings) {
        MembershipPlan membershipPlan = resolveMembershipPlan(user.getRole());

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
                .membershipCode(membershipPlan.code())
                .membershipLabel(membershipPlan.label())
                .storagePlanLabel(membershipPlan.storageLabel())
                .storageQuotaBytes(membershipPlan.storageQuotaBytes())
                .joinedAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private MembershipPlan resolveMembershipPlan(String role) {
        String normalizedRole = role == null ? "" : role.toUpperCase(Locale.ROOT);

        if (normalizedRole.contains("VIP") || normalizedRole.contains("ENTERPRISE") || normalizedRole.contains("ADMIN")) {
            return new MembershipPlan("PREMIUM", "PREMIUM MEMBER", "Premium 200GB", PREMIUM_STORAGE_BYTES);
        }

        if (normalizedRole.contains("PREMIUM") || normalizedRole.contains("PRO") || normalizedRole.contains("PLUS")) {
            return new MembershipPlan("PLUS", "PLUS MEMBER", "Plus 100GB", PLUS_STORAGE_BYTES);
        }

        return new MembershipPlan("FREE", "FREE MEMBER", "Basic 20GB", BASIC_STORAGE_BYTES);
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

        if (image.getSize() > PROFILE_IMAGE_MAX_SIZE_BYTES) {
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

    private Path resolveProfileImageDirectory() {
        return Paths.get("src", "main", "resources", "upload", "userImage")
                .toAbsolutePath()
                .normalize();
    }

    private void deleteStoredProfileImage(String storedValue) throws IOException {
        if (!isStoredProfileImageFileName(storedValue)) {
            return;
        }

        Path targetPath = resolveProfileImageDirectory().resolve(storedValue).normalize();
        Files.deleteIfExists(targetPath);
    }

    private String resolveProfileImagePreview(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }

        if (storedValue.startsWith("data:image") || storedValue.startsWith("http://") || storedValue.startsWith("https://")) {
            return storedValue;
        }

        if (!isStoredProfileImageFileName(storedValue)) {
            return null;
        }

        Path targetPath = resolveProfileImageDirectory().resolve(storedValue).normalize();
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

    private boolean isStoredProfileImageFileName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        if (value.contains("/") || value.contains("\\") || value.contains(":")) {
            return false;
        }

        return value.matches("[A-Za-z0-9._-]+");
    }

    private record MembershipPlan(String code, String label, String storageLabel, long storageQuotaBytes) {
    }
}
