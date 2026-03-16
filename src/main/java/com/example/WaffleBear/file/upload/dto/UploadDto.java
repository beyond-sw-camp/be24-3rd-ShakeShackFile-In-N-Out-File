package com.example.WaffleBear.file.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class UploadDto {

    // 파일을 업로드할 때 사용할 파일 정보들 클라이언트에서 이 내용으로 보냄
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InitReq {
        private String fileOriginName;
        private String fileFormat;
        private Long fileSize;
        private String contentType;
        private Long parentId;
        private String relativePath;
        private Long lastModified;
    }

    // 파일을 업로드할 때 사용할 파일 정보들 클라이언트 응답용
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChunkRes {
        private String fileOriginName;
        private String fileSaveName;
        private String fileFormat;
        private Long fileSize;
        private String contentType;
        private Long parentId;
        private String relativePath;
        private Long lastModified;
        private String presignedUploadUrl;
        private Integer presignedUrlExpiresIn;
        private String objectKey;
        private String finalObjectKey;
        private Integer partitionIndex;
        private Integer partitionCount;
        private Boolean partitioned;
        private Boolean uploaded;
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
        private String relativePath;
        private Long lastModified;
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
    public static class AbortReq {
        private String finalObjectKey;
        private List<String> chunkObjectKeys;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionRes {
        private String action;
        private Integer affectedCount;
    }
}
