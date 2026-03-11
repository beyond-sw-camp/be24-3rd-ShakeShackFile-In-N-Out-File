package com.example.WaffleBear.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class FileInfoDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileReq {
        @Schema(description = "파일네임")
        private String fileOriginName;
        private String fileFormat;
        private Long fileSize;
        private String contentType;
        private Long parentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileRes {
        private String fileOriginName;
        private String fileSaveName;
        private String fileFormat;
        private String presignedUploadUrl;
        private Integer presignedUrlExpiresIn;
        private String objectKey;
        private String finalObjectKey;
        private Integer partitionIndex;
        private Integer partitionCount;
        private Boolean partitioned;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteReq {
        private String fileOriginName;
        private String fileFormat;
        private Long fileSize;
        private String finalObjectKey;
        private List<String> chunkObjectKeys;
        private Long parentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteRes {
        private String fileOriginName;
        private String fileSaveName;
        private String fileFormat;
        private String finalObjectKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileListItemRes {
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
        private Integer presignedUrlExpiresIn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderReq {
        private String folderName;
        private Long parentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RenameReq {
        private String fileName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileActionRes {
        private Long targetIdx;
        private String action;
        private Integer affectedCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoveReq {
        private Long targetParentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoveBatchReq {
        private List<Long> fileIdxList;
        private Long targetParentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderPropertyRes {
        private Long idx;
        private String folderName;
        private Long parentId;
        private Integer directChildCount;
        private Integer directFileCount;
        private Integer directFolderCount;
        private Integer totalChildCount;
        private Integer totalFileCount;
        private Integer totalFolderCount;
        private Long directSize;
        private Long totalSize;
        private LocalDateTime uploadDate;
        private LocalDateTime lastModifyDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageCategoryRes {
        private String categoryKey;
        private String categoryLabel;
        private Integer fileCount;
        private Long sizeBytes;
        private Integer usagePercent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageTopFileRes {
        private Long idx;
        private String fileOriginName;
        private String fileFormat;
        private Long fileSize;
        private LocalDateTime lastModifyDate;
        private Long parentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageSummaryRes {
        private String planCode;
        private String planLabel;
        private Long quotaBytes;
        private Long usedBytes;
        private Long activeUsedBytes;
        private Long trashUsedBytes;
        private Long remainingBytes;
        private Integer usagePercent;
        private Integer totalFileCount;
        private Integer activeFileCount;
        private Integer trashFileCount;
        private Integer activeFolderCount;
        private Integer trashFolderCount;
        private List<StorageCategoryRes> categories;
        private List<StorageTopFileRes> largestFiles;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextPreviewRes {
        private Long idx;
        private String fileOriginName;
        private String fileFormat;
        private String contentType;
        private String content;
        private Boolean truncated;
        private Long fileSize;
    }
}
