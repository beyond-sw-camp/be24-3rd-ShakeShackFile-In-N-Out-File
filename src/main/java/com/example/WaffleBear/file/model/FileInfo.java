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

    private LocalDateTime uploadDate;
    private LocalDateTime lastModifyDate;

    @PrePersist
    public void prePersist() {
        this.uploadDate = LocalDateTime.now();
        this.lastModifyDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifyDate = LocalDateTime.now();
    }
}