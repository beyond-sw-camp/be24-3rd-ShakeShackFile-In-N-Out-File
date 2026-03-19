package com.example.WaffleBear.file.service;

import com.example.WaffleBear.order.model.Order;
import com.example.WaffleBear.order.repository.OrderRepository;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoragePlanService {
    private static final String DEFAULT_ADMIN_EMAIL = "administrator@administrator.adm";

    public static final long GIGABYTE = 1024L * 1024 * 1024;
    public static final long TERABYTE = 1024L * GIGABYTE;
    public static final long FREE_STORAGE_BYTES = 20L * GIGABYTE;
    public static final long PLUS_STORAGE_BYTES = 500L * GIGABYTE;
    public static final long PREMIUM_STORAGE_BYTES = 1L * TERABYTE;
    public static final long ADMIN_STORAGE_BYTES = 10L * TERABYTE;

    private static final long STANDARD_MAX_UPLOAD_FILE_BYTES = 5L * GIGABYTE;
    private static final long PREMIUM_MAX_UPLOAD_FILE_BYTES = 20L * GIGABYTE;
    private static final long ADMIN_MAX_UPLOAD_FILE_BYTES = 20L * GIGABYTE;
    private static final int FREE_MAX_UPLOAD_COUNT = 30;
    private static final int STANDARD_MAX_UPLOAD_COUNT = 100;
    private static final int PREMIUM_MAX_UPLOAD_COUNT = 300;
    private static final int ADMIN_MAX_UPLOAD_COUNT = 500;

    private static final String CATEGORY_MEMBERSHIP = "MEMBERSHIP";
    private static final String CATEGORY_STORAGE = "STORAGE";
    private static final String CATEGORY_INTERNAL = "INTERNAL";

    // 구매 가능한 플랜 정보
    private static final Map<String, ProductDefinition> PRODUCT_DEFINITIONS = Map.ofEntries(
            Map.entry("FREE", new ProductDefinition(
                    "FREE",
                    "기본 20GB",
                    "기본 멤버십",
                    CATEGORY_MEMBERSHIP,
                    FREE_STORAGE_BYTES,
                    BigDecimal.ZERO,
                    0,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    FREE_MAX_UPLOAD_COUNT
            )),
            Map.entry("PLUS", new ProductDefinition(
                    "PLUS",
                    "플러스 500GB",
                    "플러스 멤버십",
                    CATEGORY_MEMBERSHIP,
                    PLUS_STORAGE_BYTES,
                    new BigDecimal("129000"),
                    1,
                    true,
                    true,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("PREMIUM", new ProductDefinition(
                    "PREMIUM",
                    "프리미엄 1TB",
                    "프리미엄 멤버십",
                    CATEGORY_MEMBERSHIP,
                    PREMIUM_STORAGE_BYTES,
                    new BigDecimal("229000"),
                    1,
                    true,
                    true,
                    PREMIUM_MAX_UPLOAD_FILE_BYTES,
                    PREMIUM_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADDON_20GB", new ProductDefinition(
                    "ADDON_20GB",
                    "추가 20GB",
                    "추가 저장용량",
                    CATEGORY_STORAGE,
                    20L * GIGABYTE,
                    new BigDecimal("24000"),
                    1,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADDON_40GB", new ProductDefinition(
                    "ADDON_40GB",
                    "추가 40GB",
                    "추가 저장용량",
                    CATEGORY_STORAGE,
                    40L * GIGABYTE,
                    new BigDecimal("43000"),
                    1,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADDON_80GB", new ProductDefinition(
                    "ADDON_80GB",
                    "추가 80GB",
                    "추가 저장용량",
                    CATEGORY_STORAGE,
                    80L * GIGABYTE,
                    new BigDecimal("79000"),
                    1,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADDON_100GB", new ProductDefinition(
                    "ADDON_100GB",
                    "추가 100GB",
                    "추가 저장용량",
                    CATEGORY_STORAGE,
                    100L * GIGABYTE,
                    new BigDecimal("94000"),
                    1,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADDON_1TB", new ProductDefinition(
                    "ADDON_1TB",
                    "추가 1TB",
                    "추가 저장용량",
                    CATEGORY_STORAGE,
                    1L * TERABYTE,
                    new BigDecimal("249000"),
                    1,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADDON_5TB", new ProductDefinition(
                    "ADDON_5TB",
                    "추가 5TB",
                    "추가 저장용량",
                    CATEGORY_STORAGE,
                    5L * TERABYTE,
                    new BigDecimal("890000"),
                    1,
                    false,
                    false,
                    STANDARD_MAX_UPLOAD_FILE_BYTES,
                    STANDARD_MAX_UPLOAD_COUNT
            )),
            Map.entry("ADMIN", new ProductDefinition(
                    "ADMIN",
                    "관리자 10TB",
                    "관리자",
                    CATEGORY_INTERNAL,
                    ADMIN_STORAGE_BYTES,
                    BigDecimal.ZERO,
                    0,
                    true,
                    true,
                    ADMIN_MAX_UPLOAD_FILE_BYTES,
                    ADMIN_MAX_UPLOAD_COUNT
            ))
    );

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ProductDefinition requireProduct(String productCode) {
        ProductDefinition product = resolveProduct(productCode);
        if (product == null || CATEGORY_INTERNAL.equals(product.category())) {
            throw new IllegalArgumentException("unknown product code");
        }
        ensureMembershipPurchaseAllowed(product);
        return product;
    }

    public ProductDefinition resolveProduct(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return PRODUCT_DEFINITIONS.get("FREE");
        }
        return PRODUCT_DEFINITIONS.get(normalizeProductCode(productCode));
    }

    // 사용자의 idx가 널이거나, 0보다 작을 경우
    public StorageQuota resolveQuota(Long userIdx) {
        if (userIdx == null || userIdx <= 0) {
            return toQuota(PRODUCT_DEFINITIONS.get("FREE"), 0L);
        }

        User user = userRepository.findById(userIdx).orElse(null);
        return resolveQuota(user);
    }

    public StorageQuota resolveQuota(User user) {
        if (user == null) {
            return toQuota(PRODUCT_DEFINITIONS.get("FREE"), 0L);
        }

        if (isAdministrator(user)) {
            return toQuota(PRODUCT_DEFINITIONS.get("ADMIN"), 0L);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findAllByUser_Idx(user.getIdx());
        ProductDefinition membershipProduct = orders.stream()
                .filter(this::isCompletedOrder)
                .filter(order -> CATEGORY_MEMBERSHIP.equals(resolveCategory(order)))
                .filter(order -> isActive(order, now))
                .sorted(Comparator
                        .comparing((Order order) -> resolveExpiry(order, now), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Order::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::resolveProductFromOrder)
                .findFirst()
                .orElse(PRODUCT_DEFINITIONS.get("FREE"));

        long addonBytes = orders.stream()
                .filter(this::isCompletedOrder)
                .filter(order -> CATEGORY_STORAGE.equals(resolveCategory(order)))
                .filter(order -> isActive(order, now))
                .mapToLong(this::resolveQuotaBytes)
                .sum();

        return toQuota(membershipProduct, addonBytes);
    }

    public LocalDateTime calculateExpiry(ProductDefinition product, LocalDateTime purchasedAt) {
        if (product == null || product.durationYears() <= 0) {
            return null;
        }

        LocalDateTime base = purchasedAt == null ? LocalDateTime.now() : purchasedAt;
        return base.plusYears(product.durationYears());
    }

    private StorageQuota toQuota(ProductDefinition membershipProduct, long addonBytes) {
        ProductDefinition safeProduct = membershipProduct == null ? PRODUCT_DEFINITIONS.get("FREE") : membershipProduct;
        long baseQuotaBytes = safeProduct.quotaBytes();
        long extraBytes = Math.max(0L, addonBytes);
        long totalQuotaBytes = baseQuotaBytes + extraBytes;
        String planLabel = safeProduct.displayName();

        if (extraBytes > 0) {
            planLabel = planLabel + " + 추가 " + formatQuota(extraBytes);
        }

        return new StorageQuota(
                safeProduct.code(),
                safeProduct.membershipLabel(),
                planLabel,
                baseQuotaBytes,
                extraBytes,
                totalQuotaBytes,
                safeProduct.shareEnabled(),
                safeProduct.fileLockEnabled(),
                safeProduct.maxUploadFileBytes(),
                safeProduct.maxUploadCount()
        );
    }

    public boolean isAdministrator(Long userIdx) {
        if (userIdx == null || userIdx <= 0) {
            return false;
        }

        User user = userRepository.findById(userIdx).orElse(null);
        return isAdministrator(user);
    }

    public boolean isAdministrator(User user) {
        if (user == null) {
            return false;
        }

        String role = user.getRole();
        if (role != null && role.toUpperCase(Locale.ROOT).contains("ADMIN")) {
            return true;
        }

        String email = user.getEmail();
        return email != null && DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(email.trim());
    }

    private boolean isCompletedOrder(Order order) {
        return order != null && order.getPaymentId() != null && !order.getPaymentId().isBlank();
    }

    private boolean isActive(Order order, LocalDateTime now) {
        LocalDateTime expiresAt = resolveExpiry(order, now);
        return expiresAt == null || !expiresAt.isBefore(now);
    }

    private LocalDateTime resolveExpiry(Order order, LocalDateTime now) {
        if (order == null) {
            return null;
        }

        if (order.getExpiresAt() != null) {
            return order.getExpiresAt();
        }

        ProductDefinition product = resolveProductFromOrder(order);
        if (product == null || product.durationYears() <= 0) {
            return null;
        }

        LocalDateTime purchasedAt = toLocalDateTime(order.getUpdatedAt());
        if (purchasedAt == null) {
            purchasedAt = toLocalDateTime(order.getCreatedAt());
        }

        if (purchasedAt == null) {
            return null;
        }

        return purchasedAt.plusYears(product.durationYears());
    }

    private String resolveCategory(Order order) {
        if (order == null) {
            return CATEGORY_MEMBERSHIP;
        }

        if (order.getProductCategory() != null && !order.getProductCategory().isBlank()) {
            return order.getProductCategory().trim().toUpperCase(Locale.ROOT);
        }

        ProductDefinition product = resolveProductFromOrder(order);
        return product != null ? product.category() : CATEGORY_MEMBERSHIP;
    }

    private long resolveQuotaBytes(Order order) {
        if (order == null) {
            return 0L;
        }

        if (order.getQuotaBytes() != null && order.getQuotaBytes() > 0) {
            return order.getQuotaBytes();
        }

        ProductDefinition product = resolveProductFromOrder(order);
        return product != null ? product.quotaBytes() : 0L;
    }

    private ProductDefinition resolveProductFromOrder(Order order) {
        if (order == null) {
            return PRODUCT_DEFINITIONS.get("FREE");
        }

        ProductDefinition product = resolveProduct(order.getProductCode());
        if (product != null) {
            return product;
        }

        String legacyPlanType = normalizeProductCode(order.getPlanType());
        return switch (legacyPlanType) {
            case "PLUS", "PRO", "PROFESSIONAL" -> PRODUCT_DEFINITIONS.get("PLUS");
            case "PREMIUM", "VIP", "ENTERPRISE" -> PRODUCT_DEFINITIONS.get("PREMIUM");
            default -> PRODUCT_DEFINITIONS.get("FREE");
        };
    }

    private void ensureMembershipPurchaseAllowed(ProductDefinition product) {
        if (product == null || !CATEGORY_MEMBERSHIP.equals(product.category())) {
            return;
        }

        ProductDefinition currentMembership = resolveAuthenticatedMembershipProduct();
        if (currentMembership == null) {
            return;
        }

        if (membershipRank(product.code()) < membershipRank(currentMembership.code())) {
            throw new IllegalArgumentException("lower membership purchase is blocked");
        }
    }

    private ProductDefinition resolveAuthenticatedMembershipProduct() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        User user = null;

        if (principal instanceof AuthUserDetails authUserDetails && authUserDetails.getIdx() != null) {
            user = userRepository.findById(authUserDetails.getIdx()).orElse(null);
        }

        if (user == null) {
            String email = authentication.getName();
            if (email != null && !email.isBlank() && !"anonymousUser".equalsIgnoreCase(email)) {
                user = userRepository.findByEmail(email).orElse(null);
            }
        }

        if (user == null) {
            return null;
        }

        return PRODUCT_DEFINITIONS.getOrDefault(resolveQuota(user).planCode(), PRODUCT_DEFINITIONS.get("FREE"));
    }

    private int membershipRank(String productCode) {
        return switch (normalizeProductCode(productCode)) {
            case "PREMIUM", "ADMIN" -> 2;
            case "PLUS" -> 1;
            default -> 0;
        };
    }

    private String normalizeProductCode(String value) {
        return value == null ? "FREE" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String formatQuota(long quotaBytes) {
        if (quotaBytes <= 0) {
            return "0GB";
        }

        if (quotaBytes % TERABYTE == 0) {
            return (quotaBytes / TERABYTE) + "TB";
        }

        if (quotaBytes % GIGABYTE == 0) {
            return (quotaBytes / GIGABYTE) + "GB";
        }

        double quotaInGigabytes = quotaBytes / (double) GIGABYTE;
        return String.format(Locale.ROOT, "%.1fGB", quotaInGigabytes);
    }

    private LocalDateTime toLocalDateTime(Date value) {
        if (value == null) {
            return null;
        }

        return value.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public record ProductDefinition(
            String code,
            String displayName,
            String membershipLabel,
            String category,
            long quotaBytes,
            BigDecimal amount,
            int durationYears,
            boolean fileLockEnabled,
            boolean shareEnabled,
            long maxUploadFileBytes,
            int maxUploadCount
    ) {
    }

    // 레코드 형식으로 자동적으로 자동으로 getter 메서드를 생성
    // 레코드는 불변객체로 setter가 없음
    public record StorageQuota(
            String planCode,
            String membershipLabel,
            String planLabel,
            long baseQuotaBytes,
            long addonQuotaBytes,
            long totalQuotaBytes,
            boolean shareEnabled,
            boolean fileLockEnabled,
            long maxUploadFileBytes,
            int maxUploadCount
    ) {
    }
}
