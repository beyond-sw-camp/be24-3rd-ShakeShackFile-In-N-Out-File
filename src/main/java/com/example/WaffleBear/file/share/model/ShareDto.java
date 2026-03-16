package com.example.WaffleBear.file.share.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ShareDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShareReq {
        private List<Long> fileIdxList;
        private String recipientEmail;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveToDriveReq {
        private Long parentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShareInfoRes {
        private Long shareIdx;
        private Long fileIdx;
        private String fileOriginName;
        private String ownerName;
        private String ownerEmail;
        private String recipientName;
        private String recipientEmail;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SharedFileRes {
        private Long idx;
        private String fileOriginName;
        private String fileSaveName;
        private String fileSavePath;
        private String fileFormat;
        private Long fileSize;
        private String nodeType;
        private Long parentId;
        private Boolean lockedFile;
        private Boolean sharedFile;
        private Boolean trashed;
        private LocalDateTime deletedAt;
        private LocalDateTime uploadDate;
        private LocalDateTime lastModifyDate;
        private String presignedDownloadUrl;
        private String thumbnailPresignedUrl;
        private Integer presignedUrlExpiresIn;
        private Boolean sharedWithMe;
        private String ownerName;
        private String ownerEmail;
        private LocalDateTime sharedAt;
    }
}

