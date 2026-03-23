package com.example.WaffleBear.workspace.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceAssetDto {

    public record AssetRes(
            Long idx,
            Long workspaceIdx,
            String assetType,
            String originalName,
            String storedFileName,
            String objectFolder,
            String objectKey,
            String contentType,
            Long fileSize,
            String previewUrl,
            String downloadUrl,
            Integer presignedUrlExpiresIn,
            LocalDateTime createdAt
    ) {
    }

    public record ActionRes(
            Long assetIdx,
            String action
    ) {
    }

    public record AssetEvent(
            Long workspaceIdx,
            String action,
            Long actorUserIdx,
            List<AssetRes> assets,
            List<Long> assetIdxList
    ) {
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EditorJsImageRes {
        private int success;
        private FileData file;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class FileData {
            private String url;
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
