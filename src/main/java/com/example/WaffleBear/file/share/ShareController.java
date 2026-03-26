package com.example.WaffleBear.file.share;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.service.FileThumbnailQueryService;
import com.example.WaffleBear.file.service.FileThumbnailService;
import com.example.WaffleBear.file.share.model.ShareDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file/share")
@Tag(name = "Drive Share", description = "Shared drive file APIs")
@SecurityRequirement(name = "bearerAuth")
public class ShareController {

    private final ShareService shareService;
    private final FileThumbnailQueryService fileThumbnailQueryService;

    @GetMapping("/shared/list")
    @Operation(summary = "List received shared files", description = "Returns files shared with the current user.")
    public ResponseEntity<List<ShareDto.SharedFileRes>> sharedFileList(
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(shareService.sharedFileList(userDetails != null ? userDetails.getIdx() : 0L));
    }

    @GetMapping("/sent/list")
    @Operation(summary = "List sent shared files", description = "Returns files the current user shared to others.")
    public ResponseEntity<List<ShareDto.SentSharedFileRes>> sentSharedFileList(
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(shareService.sentSharedFileList(userDetails != null ? userDetails.getIdx() : 0L));
    }

    @GetMapping("/{fileIdx}")
    @Operation(summary = "Get share info", description = "Returns current share targets for a specific file.")
    public ResponseEntity<List<ShareDto.ShareInfoRes>> getShareInfo(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx) {
        return ResponseEntity.ok(shareService.getShareInfo(userDetails != null ? userDetails.getIdx() : 0L, fileIdx));
    }

    @PostMapping
    @Operation(summary = "Share files", description = "Shares one or more files with a recipient email.")
    public ResponseEntity<FileCommonDto.FileActionRes> shareFiles(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody ShareDto.ShareReq request) {
        return ResponseEntity.ok(shareService.shareFiles(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null,
                request != null ? request.recipientEmail() : null
        ));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel file share", description = "Cancels sharing for selected files and a recipient.")
    public ResponseEntity<FileCommonDto.FileActionRes> cancelShare(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody ShareDto.ShareReq request) {
        return ResponseEntity.ok(shareService.cancelShare(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null,
                request != null ? request.recipientEmail() : null
        ));
    }

    @PostMapping("/cancel-all")
    @Operation(summary = "Cancel all shares", description = "Cancels all share targets for selected files.")
    public ResponseEntity<FileCommonDto.FileActionRes> cancelAllShares(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody ShareDto.CancelAllShareReq request) {
        return ResponseEntity.ok(shareService.cancelAllShares(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null
        ));
    }

    @PostMapping("/shared/{fileIdx}/save")
    @Operation(summary = "Save shared file to drive", description = "Copies a received shared file into the current user's drive.")
    public ResponseEntity<FileCommonDto.FileListItemRes> saveSharedFileToDrive(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx,
            @RequestBody(required = false) ShareDto.SaveToDriveReq request) {
        return ResponseEntity.ok(shareService.saveSharedFileToDrive(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx,
                request != null ? request.parentId() : null
        ));
    }

    @GetMapping("/shared/{fileIdx}/text-preview")
    @Operation(summary = "Preview shared text file", description = "Returns a text preview for a received shared file.")
    public ResponseEntity<FileInfoDto.TextPreviewRes> getSharedTextPreview(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx) {
        return ResponseEntity.ok(shareService.getSharedTextPreview(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx
        ));
    }

    @GetMapping("/shared/{fileIdx}/download")
    @Operation(summary = "Download shared file", description = "Downloads a received shared file through the backend.")
    public ResponseEntity<byte[]> downloadSharedFile(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx
    ) {
        return buildDownloadResponse(
                shareService.downloadSharedFile(userDetails != null ? userDetails.getIdx() : 0L, fileIdx)
        );
    }

    @GetMapping("/shared/{fileIdx}/download-link")
    @Operation(summary = "Get shared download link", description = "Returns an attachment-oriented presigned URL for a shared file.")
    public ResponseEntity<FileCommonDto.FileDownloadUrlRes> getSharedDownloadLink(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx
    ) {
        return ResponseEntity.ok(new FileCommonDto.FileDownloadUrlRes(
                shareService.getSharedFileDownloadUrl(userDetails != null ? userDetails.getIdx() : 0L, fileIdx)
        ));
    }

    @GetMapping("/shared/{fileIdx}/thumbnail")
    @Operation(summary = "Get shared thumbnail", description = "Returns a cached thumbnail for a received shared file.")
    public ResponseEntity<byte[]> getSharedThumbnail(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        FileThumbnailService.ThumbnailPayload payload = fileThumbnailQueryService.loadSharedThumbnail(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx
        );
        return buildThumbnailResponse(payload, ifNoneMatch);
    }

    private ResponseEntity<byte[]> buildThumbnailResponse(
            FileThumbnailService.ThumbnailPayload payload,
            String ifNoneMatch
    ) {
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofDays(1)).cachePrivate().mustRevalidate();
        if (payload == null || payload.bytes() == null || payload.bytes().length == 0) {
            return ResponseEntity.noContent()
                    .cacheControl(cacheControl)
                    .build();
        }

        if (matchesEtag(ifNoneMatch, payload.eTag())) {
            return ResponseEntity.status(304)
                    .cacheControl(cacheControl)
                    .eTag(payload.eTag())
                    .lastModified(payload.lastModifiedEpochMillis())
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .eTag(payload.eTag())
                .lastModified(payload.lastModifiedEpochMillis())
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.bytes());
    }

    private ResponseEntity<byte[]> buildDownloadResponse(FileCommonDto.FileDownloadPayload payload) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(payload.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentLength(payload.contentLength() != null ? payload.contentLength() : payload.bytes().length)
                .contentType(resolveMediaType(payload.contentType()))
                .body(payload.bytes());
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean matchesEtag(String ifNoneMatch, String eTag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank() || eTag == null || eTag.isBlank()) {
            return false;
        }

        return Arrays.stream(ifNoneMatch.split(","))
                .map(String::trim)
                .map(this::normalizeEtag)
                .anyMatch(eTag::equals);
    }

    private String normalizeEtag(String rawEtag) {
        String normalized = rawEtag == null ? "" : rawEtag.trim();
        if (normalized.startsWith("W/")) {
            normalized = normalized.substring(2).trim();
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }
}
