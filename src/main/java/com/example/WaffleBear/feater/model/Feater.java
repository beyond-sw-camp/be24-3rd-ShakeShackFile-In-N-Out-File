package com.example.WaffleBear.feater.model;

import com.example.WaffleBear.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx", unique = true, nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 10)
    private String localeCode;

    @Column(nullable = false, length = 10)
    private String regionCode;

    @Column(nullable = false)
    private Boolean marketingOptIn;

    @Column(nullable = false)
    private Boolean privateProfile;

    @Column(nullable = false)
    private Boolean emailNotification;

    @Column(nullable = false)
    private Boolean securityNotification;

    private String profileImageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (displayName == null || displayName.isBlank()) {
            displayName = "User";
        }
        if (localeCode == null || localeCode.isBlank()) {
            localeCode = "KO";
        }
        if (regionCode == null || regionCode.isBlank()) {
            regionCode = "KR";
        }
        if (marketingOptIn == null) {
            marketingOptIn = true;
        }
        if (privateProfile == null) {
            privateProfile = false;
        }
        if (emailNotification == null) {
            emailNotification = true;
        }
        if (securityNotification == null) {
            securityNotification = true;
        }

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(
            String displayName,
            String localeCode,
            String regionCode,
            Boolean marketingOptIn,
            Boolean privateProfile,
            Boolean emailNotification,
            Boolean securityNotification,
            String profileImageUrl
    ) {
        this.displayName = displayName;
        this.localeCode = localeCode;
        this.regionCode = regionCode;
        this.marketingOptIn = marketingOptIn;
        this.privateProfile = privateProfile;
        this.emailNotification = emailNotification;
        this.securityNotification = securityNotification;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
