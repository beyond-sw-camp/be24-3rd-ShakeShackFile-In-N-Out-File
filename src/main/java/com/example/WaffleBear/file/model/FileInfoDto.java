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
        private Boolean lockedFile;
        private Boolean sharedFile;
        private LocalDateTime uploadDate;
        private LocalDateTime lastModifyDate;
        private String presignedDownloadUrl;
        private Integer presignedUrlExpiresIn;
    }
}
