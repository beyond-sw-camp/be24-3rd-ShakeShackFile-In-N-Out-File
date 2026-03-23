package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetDto;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workspace")
public class WorkspaceAssetController {

    private final WorkspaceAssetService workspaceAssetService;

    @GetMapping("/{workspaceId}/assets")
    public ResponseEntity<List<WorkspaceAssetDto.AssetRes>> listAssets(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId
    ) {
        return ResponseEntity.ok(
                workspaceAssetService.listAssets(user != null ? user.getIdx() : 0L, workspaceId)
        );
    }

    @PostMapping(value = "/{workspaceId}/assets/editorjs",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity uploadEditorJsImage(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @RequestParam("image") MultipartFile image
    ) {
        WorkspaceAssetService.EditorJsUploadResult
                file = workspaceAssetService.uploadAssets(user.getIdx(),workspaceId, image);

        return ResponseEntity.ok(Map.of(
                "success", 1,
                "file", Map.of("url", file.fileUrl(),
                        "assetIdx", file.assetIdx())));
    }

    @DeleteMapping(value = "/{workspaceId}/assets/{assetIdx}/editorjs",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteEditorJsImage(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetIdx      // ✅ assetIdx 로 삭제
    ) {
        workspaceAssetService.deleteEditorJsImage(user.getIdx(), workspaceId, assetIdx);
        return ResponseEntity.ok(Map.of("success", 1));
    }

    @PostMapping("/{workspaceId}/assets/{assetId}/save-to-drive")
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
}
