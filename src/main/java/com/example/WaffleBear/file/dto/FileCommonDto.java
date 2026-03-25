package com.example.WaffleBear.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class FileCommonDto {

    @Schema(description = "파일 목록 항목 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileListItemRes {
        @Schema(description = "파일 고유 ID", example = "1")
        private Long idx;
        @Schema(description = "원본 파일명", example = "보고서.pdf")
        private String fileOriginName;
        @Schema(description = "저장된 파일명", example = "abc123-def456.pdf")
        private String fileSaveName;
        @Schema(description = "파일 저장 경로", example = "/user/1/files/")
        private String fileSavePath;
        @Schema(description = "파일 확장자", example = "pdf")
        private String fileFormat;
        @Schema(description = "파일 크기 (바이트)", example = "1048576")
        private Long fileSize;
        @Schema(description = "노드 유형 (FILE 또는 FOLDER)", example = "FILE")
        private String nodeType;
        @Schema(description = "상위 폴더 ID (루트인 경우 null)", example = "5")
        private Long parentId;
        @Schema(description = "파일 잠금 여부", example = "false")
        private Boolean lockedFile;
        @Schema(description = "파일 공유 여부", example = "false")
        private Boolean sharedFile;
        @Schema(description = "휴지통 이동 여부", example = "false")
        private Boolean trashed;
        @Schema(description = "삭제 일시")
        private LocalDateTime deletedAt;
        @Schema(description = "업로드 일시")
        private LocalDateTime uploadDate;
        @Schema(description = "최종 수정 일시")
        private LocalDateTime lastModifyDate;
        @Schema(description = "다운로드용 Presigned URL")
        private String presignedDownloadUrl;
        @Schema(description = "썸네일 Presigned URL")
        private String thumbnailPresignedUrl;
        @Schema(description = "Presigned URL 만료 시간 (초)", example = "3600")
        private Integer presignedUrlExpiresIn;
    }

    @Schema(description = "파일 작업 결과 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileActionRes {
        @Schema(description = "대상 파일 ID", example = "1")
        private Long targetIdx;
        @Schema(description = "수행된 작업", example = "SHARE")
        private String action;
        @Schema(description = "영향받은 파일 수", example = "3")
        private Integer affectedCount;
    }
}
