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
    public static class FileActionRes {
        private Long targetIdx;
        private String action;
        private Integer affectedCount;
    }
}
