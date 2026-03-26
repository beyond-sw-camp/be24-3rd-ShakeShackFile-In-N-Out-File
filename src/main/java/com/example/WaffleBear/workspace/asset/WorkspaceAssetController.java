package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workspace")
@Tag(name = "Workspace Assets", description = "Workspace attachment APIs")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceAssetController {

    private final WorkspaceAssetService workspaceAssetService;

    /**
     * 에셋 목록 조회
     */
    @GetMapping("/{workspaceId}/assets")
    @Operation(summary = "List workspace assets", description = "Returns assets attached to a workspace.")
    public ResponseEntity<List<WorkspaceAssetDto.AssetRes>> listAssets(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId
    ) {
        return ResponseEntity.ok(
                workspaceAssetService.listAssets(
                        user != null ? user.getIdx() : 0L,
                        workspaceId
                )
        );
    }

    /**
     * 여러 파일 업로드 (일반 에셋용)
     */
    @PostMapping("/{workspaceId}/assets")
    @Operation(summary = "Upload workspace assets", description = "Uploads one or more regular workspace assets.")
    public BaseResponse<?> uploadWorkspaceAssets(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @RequestParam("files") MultipartFile[] files
    ) {
        List<WorkspaceAssetDto.AssetRes> result = workspaceAssetService.uploadWorkspaceAssets(
                user.getIdx(),
                workspaceId,
                files
        );
        return BaseResponse.success(result);  // ✅ BaseResponse로 감싸기
    }

    @PostMapping(value = "/{workspaceId}/assets/editorjs",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload Editor.js asset", description = "Uploads an asset for Editor.js blocks inside a workspace.")
    public ResponseEntity uploadEditorJsImage(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @RequestParam("image") MultipartFile image
    ) {
        WorkspaceAssetService.EditorJsUploadResult
                file = workspaceAssetService.uploadAssetsEditorJs(user.getIdx(),workspaceId, image);

        return ResponseEntity.ok(Map.of(
                "success", 1,
                "file", Map.of("url", file.fileUrl(),
                        "assetIdx", file.assetIdx())));
    }

    @DeleteMapping(value = "/{workspaceId}/assets/{assetIdx}/editorjs",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete Editor.js asset", description = "Deletes an Editor.js asset from a workspace.")
    public ResponseEntity<?> deleteEditorJsImage(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetIdx      // ✅ assetIdx 로 삭제
    ) {
        workspaceAssetService.deleteEditorJsImage(user.getIdx(), workspaceId, assetIdx);
        return ResponseEntity.ok(Map.of("success", 1));
    }
    /**
     * 일반 에셋 삭제
     */
    @DeleteMapping("/{workspaceId}/assets/{assetId}")
    @Operation(summary = "Delete workspace asset", description = "Deletes a regular workspace asset.")
    public ResponseEntity<?> deleteWorkspaceAsset(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetId
    ) {
        workspaceAssetService.deleteWorkspaceAsset(user.getIdx(), workspaceId, assetId);
        return ResponseEntity.ok(Map.of("success", 1));
    }


    @PostMapping("/{workspaceId}/assets/{assetId}/save-to-drive")
    @Operation(summary = "Save asset to drive", description = "Copies a workspace asset into the user's drive.")
    public ResponseEntity<FileCommonDto.FileListItemRes> saveAssetToDrive(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetId,
            @RequestParam(required = false) Long parentId
    ) {
        return ResponseEntity.ok(
                workspaceAssetService.saveAssetToDrive(
                        user != null ? user.getIdx() : 0L,
                        workspaceId,
                        assetId,
                        parentId
                )
        );
    }

    @GetMapping("/{workspaceId}/assets/{assetId}/download")
    @Operation(summary = "Download workspace asset", description = "Downloads a workspace asset through the backend.")
    public ResponseEntity<byte[]> downloadWorkspaceAsset(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetId
    ) {
        return buildDownloadResponse(
                workspaceAssetService.downloadWorkspaceAsset(
                        user != null ? user.getIdx() : 0L,
                        workspaceId,
                        assetId
                )
        );
    }

    @GetMapping("/{workspaceId}/assets/{assetId}/download-link")
    @Operation(summary = "Get workspace asset download link", description = "Returns an attachment-oriented presigned URL for a workspace asset.")
    public ResponseEntity<FileCommonDto.FileDownloadUrlRes> getWorkspaceAssetDownloadLink(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetId
    ) {
        return ResponseEntity.ok(new FileCommonDto.FileDownloadUrlRes(
                workspaceAssetService.getWorkspaceAssetDownloadUrl(
                        user != null ? user.getIdx() : 0L,
                        workspaceId,
                        assetId
                )
        ));
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
}
