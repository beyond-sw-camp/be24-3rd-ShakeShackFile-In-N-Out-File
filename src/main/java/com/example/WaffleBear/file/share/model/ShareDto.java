package com.example.WaffleBear.file.share.model;

import java.time.LocalDateTime;
import java.util.List;

public class ShareDto {

    public record ShareReq(
            List<Long> fileIdxList,
            String recipientEmail
    ) {
    }

    public record CancelAllShareReq(
            List<Long> fileIdxList
    ) {
    }

    public record SaveToDriveReq(
            Long parentId
    ) {
    }

    public record ShareInfoRes(
            Long shareIdx,
            Long fileIdx,
            String fileOriginName,
            String ownerName,
            String ownerEmail,
            String recipientName,
            String recipientEmail,
            LocalDateTime createdAt
    ) {
    }

    public record SharedFileRes(
            Long idx,
            String fileOriginName,
            String fileSaveName,
            String fileSavePath,
            String fileFormat,
            Long fileSize,
            String nodeType,
            Long parentId,
            Boolean lockedFile,
            Boolean sharedFile,
            Boolean trashed,
            LocalDateTime deletedAt,
            LocalDateTime uploadDate,
            LocalDateTime lastModifyDate,
            String presignedDownloadUrl,
            String thumbnailPresignedUrl,
            Integer presignedUrlExpiresIn,
            Boolean sharedWithMe,
            String ownerName,
            String ownerEmail,
            LocalDateTime sharedAt
    ) {
    }

    public record ShareRecipientRes(
            String recipientName,
            String recipientEmail,
            LocalDateTime sharedAt
    ) {
    }

    public record SentSharedFileRes(
            Long idx,
            String fileOriginName,
            String fileSaveName,
            String fileSavePath,
            String fileFormat,
            Long fileSize,
            String nodeType,
            Long parentId,
            Boolean lockedFile,
            Boolean sharedFile,
            Boolean trashed,
            LocalDateTime deletedAt,
            LocalDateTime uploadDate,
            LocalDateTime lastModifyDate,
            String presignedDownloadUrl,
            String thumbnailPresignedUrl,
            Integer presignedUrlExpiresIn,
            Boolean sharedWithMe,
            String ownerName,
            String ownerEmail,
            LocalDateTime sharedAt,
            Integer recipientCount,
            List<ShareRecipientRes> recipients
    ) {
    }
}
