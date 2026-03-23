package com.example.WaffleBear.workspace.asset.model;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
public class WorkspaceAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_idx", nullable = false)
    private Post workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_idx", nullable = false)
    private User uploader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkspaceAssetType assetType;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String objectFolder;

    @Column(nullable = false, unique = true)
    private String objectKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
