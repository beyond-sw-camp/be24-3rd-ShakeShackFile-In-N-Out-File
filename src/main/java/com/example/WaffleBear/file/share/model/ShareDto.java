package com.example.WaffleBear.file.share.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class ShareDto {

    @Schema(description = "파일 공유 / 공유 취소 요청")
    public record ShareReq(
            @Schema(description = "공유 대상 파일 ID 목록", example = "[1, 2, 3]")
            List<Long> fileIdxList,
            @Schema(description = "공유 대상 사용자 이메일", example = "user@example.com")
            String recipientEmail
    ) {
    }

    @Schema(description = "전체 공유 취소 요청")
    public record CancelAllShareReq(
            @Schema(description = "전체 공유 취소할 파일 ID 목록", example = "[1, 2, 3]")
            List<Long> fileIdxList
    ) {
    }

    @Schema(description = "공유 파일 드라이브 저장 요청")
    public record SaveToDriveReq(
            @Schema(description = "저장할 대상 폴더 ID (선택)", example = "10")
            Long parentId
    ) {
    }

    @Schema(description = "파일 공유 정보 응답")
    public record ShareInfoRes(
            @Schema(description = "공유 고유 ID", example = "1")
            Long shareIdx,
            @Schema(description = "파일 고유 ID", example = "5")
            Long fileIdx,
            @Schema(description = "원본 파일명", example = "보고서.pdf")
            String fileOriginName,
            @Schema(description = "파일 소유자 이름", example = "홍길동")
            String ownerName,
            @Schema(description = "파일 소유자 이메일", example = "owner@example.com")
            String ownerEmail,
            @Schema(description = "공유 대상자 이름", example = "김철수")
            String recipientName,
            @Schema(description = "공유 대상자 이메일", example = "recipient@example.com")
            String recipientEmail,
            @Schema(description = "공유 일시")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "공유받은 파일 응답")
    public record SharedFileRes(
            @Schema(description = "파일 고유 ID", example = "1")
            Long idx,
            @Schema(description = "원본 파일명", example = "보고서.pdf")
            String fileOriginName,
            @Schema(description = "저장된 파일명", example = "abc123.pdf")
            String fileSaveName,
            @Schema(description = "파일 저장 경로")
            String fileSavePath,
            @Schema(description = "파일 확장자", example = "pdf")
            String fileFormat,
            @Schema(description = "파일 크기 (바이트)", example = "1048576")
            Long fileSize,
            @Schema(description = "노드 유형", example = "FILE")
            String nodeType,
            @Schema(description = "상위 폴더 ID", example = "5")
            Long parentId,
            @Schema(description = "파일 잠금 여부", example = "false")
            Boolean lockedFile,
            @Schema(description = "파일 공유 여부", example = "true")
            Boolean sharedFile,
            @Schema(description = "휴지통 이동 여부", example = "false")
            Boolean trashed,
            @Schema(description = "삭제 일시")
            LocalDateTime deletedAt,
            @Schema(description = "업로드 일시")
            LocalDateTime uploadDate,
            @Schema(description = "최종 수정 일시")
            LocalDateTime lastModifyDate,
            @Schema(description = "다운로드용 Presigned URL")
            String presignedDownloadUrl,
            @Schema(description = "썸네일 Presigned URL")
            String thumbnailPresignedUrl,
            @Schema(description = "Presigned URL 만료 시간 (초)", example = "3600")
            Integer presignedUrlExpiresIn,
            @Schema(description = "나에게 공유된 파일 여부", example = "true")
            Boolean sharedWithMe,
            @Schema(description = "소유자 이름", example = "홍길동")
            String ownerName,
            @Schema(description = "소유자 이메일", example = "owner@example.com")
            String ownerEmail,
            @Schema(description = "공유 일시")
            LocalDateTime sharedAt
    ) {
    }

    @Schema(description = "공유 대상자 정보")
    public record ShareRecipientRes(
            @Schema(description = "대상자 이름", example = "김철수")
            String recipientName,
            @Schema(description = "대상자 이메일", example = "recipient@example.com")
            String recipientEmail,
            @Schema(description = "공유 일시")
            LocalDateTime sharedAt
    ) {
    }

    @Schema(description = "내가 공유한 파일 응답")
    public record SentSharedFileRes(
            @Schema(description = "파일 고유 ID", example = "1")
            Long idx,
            @Schema(description = "원본 파일명", example = "보고서.pdf")
            String fileOriginName,
            @Schema(description = "저장된 파일명", example = "abc123.pdf")
            String fileSaveName,
            @Schema(description = "파일 저장 경로")
            String fileSavePath,
            @Schema(description = "파일 확장자", example = "pdf")
            String fileFormat,
            @Schema(description = "파일 크기 (바이트)", example = "1048576")
            Long fileSize,
            @Schema(description = "노드 유형", example = "FILE")
            String nodeType,
            @Schema(description = "상위 폴더 ID", example = "5")
            Long parentId,
            @Schema(description = "파일 잠금 여부", example = "false")
            Boolean lockedFile,
            @Schema(description = "파일 공유 여부", example = "true")
            Boolean sharedFile,
            @Schema(description = "휴지통 이동 여부", example = "false")
            Boolean trashed,
            @Schema(description = "삭제 일시")
            LocalDateTime deletedAt,
            @Schema(description = "업로드 일시")
            LocalDateTime uploadDate,
            @Schema(description = "최종 수정 일시")
            LocalDateTime lastModifyDate,
            @Schema(description = "다운로드용 Presigned URL")
            String presignedDownloadUrl,
            @Schema(description = "썸네일 Presigned URL")
            String thumbnailPresignedUrl,
            @Schema(description = "Presigned URL 만료 시간 (초)", example = "3600")
            Integer presignedUrlExpiresIn,
            @Schema(description = "나에게 공유된 파일 여부", example = "false")
            Boolean sharedWithMe,
            @Schema(description = "소유자 이름", example = "홍길동")
            String ownerName,
            @Schema(description = "소유자 이메일", example = "owner@example.com")
            String ownerEmail,
            @Schema(description = "공유 일시")
            LocalDateTime sharedAt,
            @Schema(description = "공유 대상자 수", example = "3")
            Integer recipientCount,
            @Schema(description = "공유 대상자 목록")
            List<ShareRecipientRes> recipients
    ) {
    }
}
