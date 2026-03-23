package com.example.WaffleBear.file.model;

import com.example.WaffleBear.user.model.User;
import jakarta.persistence.*;
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
public class FileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_idx")
    private FileInfo parent;

    @Enumerated(EnumType.STRING)
    private FileNodeType nodeType;

    @Column(nullable = false)
    private String fileOriginName;
    @Column(nullable = false)
    private String fileFormat;
    @Column(nullable = false)
    private String fileSaveName;

    private String fileSavePath;

    private Long fileSize;

    private boolean lockedFile;
    private boolean sharedFile;
    private Boolean trashed;
    private LocalDateTime deletedAt;

    private LocalDateTime uploadDate;
    private LocalDateTime lastModifyDate;

    @PrePersist
    public void prePersist() {
        if (this.nodeType == null) {
            this.nodeType = FileNodeType.FILE;
        }
        if (this.trashed == null) {
            this.trashed = false;
        }
        this.uploadDate = LocalDateTime.now();
        this.lastModifyDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifyDate = LocalDateTime.now();
    }

    public void markTrashed(LocalDateTime deletedAt) {
        this.trashed = true;
        this.deletedAt = deletedAt;
    }

    public void restore() {
        this.trashed = false;
        this.deletedAt = null;
    }

    public boolean isTrashed() {
        return Boolean.TRUE.equals(this.trashed);
    }

    public void changeParent(FileInfo parent) {
        this.parent = parent;
    }

    public void rename(String fileOriginName) {
        this.fileOriginName = fileOriginName;
    }

    public void changeLockedFile(boolean lockedFile) {
        this.lockedFile = lockedFile;
    }

    public void changeSharedFile(boolean sharedFile) {
        this.sharedFile = sharedFile;
    }
}
