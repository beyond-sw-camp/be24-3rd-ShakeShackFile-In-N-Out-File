package com.example.WaffleBear.workspace.asset.model;

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
}
