package com.example.WaffleBear.file.info.dto;

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
        private Boolean adminAccount;
        private Boolean shareEnabled;
        private Boolean fileLockEnabled;
        private Long maxUploadFileBytes;
        private Integer maxUploadCount;
        private Long quotaBytes;
        private Long baseQuotaBytes;
        private Long addonQuotaBytes;
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
