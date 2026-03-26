package com.example.WaffleBear.file.manage;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import com.example.WaffleBear.file.service.FileThumbnailQueryService;
import com.example.WaffleBear.file.service.FileThumbnailService;
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
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
@Tag(name = "Drive Files", description = "Drive file and folder management APIs")
@SecurityRequirement(name = "bearerAuth")
public class FileManageController {

    private final FileManageService fileManageService;
    private final FileThumbnailQueryService fileThumbnailQueryService;

    @GetMapping("/list")
    @Operation(summary = "List drive items", description = "Returns the current user's drive items.")
    public ResponseEntity<List<FileCommonDto.FileListItemRes>> fileList(
            @AuthenticationPrincipal AuthUserDetails dto
    ) {
        // dto확인 즉, 로그인 상태 확인
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.list(userIdx));
    }

    @GetMapping("/list/page")
    @Operation(summary = "List drive items by page", description = "Returns paged drive items with filtering and sorting.")
    public ResponseEntity<FileCommonDto.FileListPageRes> fileListPage(
            @AuthenticationPrincipal AuthUserDetails dto,
            @ModelAttribute FileManageDto.ListPageReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.listPage(userIdx, request));
    }

    @GetMapping("/{fileIdx}/download")
    @Operation(summary = "Download file", description = "Downloads a drive file through the backend.")
    public ResponseEntity<byte[]> downloadFile(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return buildDownloadResponse(fileManageService.downloadFile(userIdx, fileIdx));
    }

    @GetMapping("/{fileIdx}/download-link")
    @Operation(summary = "Get download link", description = "Returns an attachment-oriented presigned download URL.")
    public ResponseEntity<FileCommonDto.FileDownloadUrlRes> getDownloadLink(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(new FileCommonDto.FileDownloadUrlRes(
                fileManageService.getDownloadUrl(userIdx, fileIdx)
        ));
    }

    @GetMapping("/{fileIdx}/thumbnail")
    @Operation(summary = "Get thumbnail", description = "Returns a cached thumbnail for a drive file when available.")
    public ResponseEntity<byte[]> getThumbnail(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        FileThumbnailService.ThumbnailPayload payload = fileThumbnailQueryService.loadOwnedThumbnail(userIdx, fileIdx);
        return buildThumbnailResponse(payload, ifNoneMatch);
    }

    @PostMapping("/folder")
    @Operation(summary = "Create folder", description = "Creates a new folder in the drive tree.")
    public ResponseEntity<FileCommonDto.FileListItemRes> createFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileManageDto.FolderReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.createFolder(userIdx, request));
    }

    @PatchMapping("/{fileIdx}/trash")
    @Operation(summary = "Move to trash", description = "Moves a file or folder into the trash.")
    public ResponseEntity<FileCommonDto.FileActionRes> moveToTrash(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveToTrash(userIdx, fileIdx));
    }

    @PatchMapping("/{fileIdx}/restore")
    @Operation(summary = "Restore from trash", description = "Restores a trashed file or folder.")
    public ResponseEntity<FileCommonDto.FileActionRes> restoreFromTrash(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.restoreFromTrash(userIdx, fileIdx));
    }

    @DeleteMapping("/{fileIdx}")
    @Operation(summary = "Delete permanently", description = "Permanently deletes a file or folder.")
    public ResponseEntity<FileCommonDto.FileActionRes> deletePermanently(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.deletePermanently(userIdx, fileIdx));
    }

    @DeleteMapping("/trash")
    @Operation(summary = "Clear trash", description = "Permanently deletes all items in the trash.")
    public ResponseEntity<FileCommonDto.FileActionRes> clearTrash(
            @AuthenticationPrincipal AuthUserDetails dto
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.clearTrash(userIdx));
    }

    @PatchMapping("/{fileIdx}/move")
    @Operation(summary = "Move item", description = "Moves a single file or folder to another parent folder.")
    public ResponseEntity<FileCommonDto.FileActionRes> moveToFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx,
            @RequestBody FileManageDto.MoveReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveToFolder(
                userIdx,
                fileIdx,
                request != null ? request.getTargetParentId() : null
        ));
    }

    @PatchMapping("/{fileIdx}/rename")
    @Operation(summary = "Rename item", description = "Renames a single file or folder.")
    public ResponseEntity<FileCommonDto.FileListItemRes> renameFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx,
            @RequestBody FileManageDto.RenameReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.renameFolder(
                userIdx,
                fileIdx,
                request != null ? request.getFileName() : null
        ));
    }

    @PatchMapping("/move")
    @Operation(summary = "Move items in batch", description = "Moves multiple drive items to another folder.")
    public ResponseEntity<FileCommonDto.FileActionRes> moveFilesToFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileManageDto.MoveBatchReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveFilesToFolder(
                userIdx,
                request != null ? request.getFileIdxList() : null,
                request != null ? request.getTargetParentId() : null
        ));
    }

    @PatchMapping("/restore")
    @Operation(summary = "Restore items in batch", description = "Restores multiple trashed drive items.")
    public ResponseEntity<FileCommonDto.FileActionRes> restoreFilesFromTrash(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileManageDto.RestoreBatchReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.restoreFilesFromTrash(
                userIdx,
                request != null ? request.getFileIdxList() : null
        ));
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
