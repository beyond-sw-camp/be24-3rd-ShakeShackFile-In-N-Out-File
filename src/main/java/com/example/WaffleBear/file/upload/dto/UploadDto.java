package com.example.WaffleBear.file.upload.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class UploadDto {

    @Schema(description = "업로드 초기화 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InitReq {
        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String fileOriginName;
        @Schema(description = "파일 확장자", example = "pdf")
        private String fileFormat;
        @Schema(description = "파일 크기 (바이트)", example = "1048576")
        private Long fileSize;
        @Schema(description = "Content-Type", example = "application/pdf")
        private String contentType;
        @Schema(description = "상위 폴더 ID (루트인 경우 null)", example = "5")
        private Long parentId;
        @Schema(description = "상대 경로 (폴더 업로드 시)", example = "docs/reports/")
        private String relativePath;
        @Schema(description = "최종 수정 시간 (Unix timestamp ms)", example = "1711234567890")
        private Long lastModified;
    }

    @Schema(description = "청크 업로드 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChunkRes {
        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String fileOriginName;
        @Schema(description = "저장된 파일명", example = "abc123.pdf")
        private String fileSaveName;
        @Schema(description = "파일 확장자", example = "pdf")
        private String fileFormat;
        @Schema(description = "파일 크기 (바이트)", example = "1048576")
        private Long fileSize;
        @Schema(description = "Content-Type", example = "application/pdf")
        private String contentType;
        @Schema(description = "상위 폴더 ID", example = "5")
        private Long parentId;
        @Schema(description = "상대 경로", example = "docs/reports/")
        private String relativePath;
        @Schema(description = "최종 수정 시간 (Unix timestamp ms)", example = "1711234567890")
        private Long lastModified;
        @Schema(description = "업로드용 Presigned URL")
        private String presignedUploadUrl;
        @Schema(description = "Presigned URL 만료 시간 (초)", example = "3600")
        private Integer presignedUrlExpiresIn;
        @Schema(description = "오브젝트 키", example = "tmp/abc123.pdf")
        private String objectKey;
        @Schema(description = "최종 오브젝트 키", example = "user/1/abc123.pdf")
        private String finalObjectKey;
        @Schema(description = "청크 인덱스", example = "0")
        private Integer partitionIndex;
        @Schema(description = "전체 청크 수", example = "5")
        private Integer partitionCount;
        @Schema(description = "분할 업로드 여부", example = "true")
        private Boolean partitioned;
        @Schema(description = "업로드 완료 여부", example = "false")
        private Boolean uploaded;
    }

    @Schema(description = "업로드 완료 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteReq {
        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String fileOriginName;
        @Schema(description = "파일 확장자", example = "pdf")
        private String fileFormat;
        @Schema(description = "파일 크기 (바이트)", example = "1048576")
        private Long fileSize;
        @Schema(description = "최종 오브젝트 키", example = "user/1/abc123.pdf")
        private String finalObjectKey;
        @Schema(description = "청크 오브젝트 키 목록")
        private List<String> chunkObjectKeys;
        @Schema(description = "상위 폴더 ID", example = "5")
        private Long parentId;
        @Schema(description = "상대 경로", example = "docs/reports/")
        private String relativePath;
        @Schema(description = "최종 수정 시간 (Unix timestamp ms)", example = "1711234567890")
        private Long lastModified;
    }

    @Schema(description = "업로드 완료 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteRes {
        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String fileOriginName;
        @Schema(description = "저장된 파일명", example = "abc123.pdf")
        private String fileSaveName;
        @Schema(description = "파일 확장자", example = "pdf")
        private String fileFormat;
        @Schema(description = "최종 오브젝트 키", example = "user/1/abc123.pdf")
        private String finalObjectKey;
    }

    @Schema(description = "업로드 취소 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AbortReq {
        @Schema(description = "최종 오브젝트 키", example = "user/1/abc123.pdf")
        private String finalObjectKey;
        @Schema(description = "삭제할 청크 오브젝트 키 목록")
        private List<String> chunkObjectKeys;
    }

    @Schema(description = "업로드 작업 결과 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionRes {
        @Schema(description = "수행된 작업", example = "ABORT")
        private String action;
        @Schema(description = "영향받은 파일 수", example = "3")
        private Integer affectedCount;
    }
}
