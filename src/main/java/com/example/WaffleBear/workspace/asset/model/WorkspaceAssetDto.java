package com.example.WaffleBear.workspace.asset.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceAssetDto {

    @Schema(description = "에셋 응답")
    public record AssetRes(
            @Schema(description = "에셋 고유 ID", example = "1")
            Long idx,
            @Schema(description = "워크스페이스 ID", example = "10")
            Long workspaceIdx,
            @Schema(description = "에셋 유형 (IMAGE, FILE 등)", example = "IMAGE")
            String assetType,
            @Schema(description = "원본 파일명", example = "스크린샷.png")
            String originalName,
            @Schema(description = "저장된 파일명", example = "abc123.png")
            String storedFileName,
            @Schema(description = "오브젝트 폴더 경로")
            String objectFolder,
            @Schema(description = "오브젝트 키")
            String objectKey,
            @Schema(description = "Content-Type", example = "image/png")
            String contentType,
            @Schema(description = "파일 크기 (바이트)", example = "204800")
            Long fileSize,
            @Schema(description = "미리보기 URL")
            String previewUrl,
            @Schema(description = "다운로드 URL")
            String downloadUrl,
            @Schema(description = "Presigned URL 만료 시간 (초)", example = "3600")
            Integer presignedUrlExpiresIn,
            @Schema(description = "에셋 생성 일시")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "에셋 작업 결과 응답")
    public record ActionRes(
            @Schema(description = "에셋 ID", example = "1")
            Long assetIdx,
            @Schema(description = "수행된 작업", example = "DELETE")
            String action
    ) {
    }

    @Schema(description = "에셋 이벤트 (SSE/WebSocket 전송용)")
    public record AssetEvent(
            @Schema(description = "워크스페이스 ID", example = "10")
            Long workspaceIdx,
            @Schema(description = "이벤트 유형", example = "UPLOAD")
            String action,
            @Schema(description = "수행자 IDX", example = "1")
            Long actorUserIdx,
            @Schema(description = "관련 에셋 목록")
            List<AssetRes> assets,
            @Schema(description = "관련 에셋 ID 목록", example = "[1, 2, 3]")
            List<Long> assetIdxList
    ) {
    }

    @Schema(description = "EditorJS 이미지 업로드 응답")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EditorJsImageRes {
        @Schema(description = "업로드 성공 여부 (1: 성공)", example = "1")
        private int success;
        @Schema(description = "업로드된 파일 정보")
        private FileData file;

        @Schema(description = "EditorJS 파일 데이터")
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class FileData {
            @Schema(description = "이미지 URL")
            private String url;
            @Schema(description = "에셋 IDX", example = "1")
            private Long assetIdx;
        }

        public static EditorJsImageRes from(AssetRes asset) {
            return EditorJsImageRes.builder()
                    .success(1)
                    .file(FileData.builder()
                            .url(asset.previewUrl())
                            .assetIdx(asset.idx())
                            .build())
                    .build();
        }
    }
}
