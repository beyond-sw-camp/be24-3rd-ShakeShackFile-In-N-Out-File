package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.user.model.AuthUserDetails;
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

    @PostMapping(value = "/{workspaceId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<WorkspaceAssetDto.AssetRes>> uploadAssets(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(
                workspaceAssetService.uploadAssets(user != null ? user.getIdx() : 0L, workspaceId, files)
        );
    }

    @DeleteMapping("/{workspaceId}/assets/{assetId}")
    public ResponseEntity<WorkspaceAssetDto.ActionRes> deleteAsset(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long workspaceId,
            @PathVariable Long assetId
    ) {
        return ResponseEntity.ok(
                workspaceAssetService.deleteAsset(user != null ? user.getIdx() : 0L, workspaceId, assetId)
        );
    }
}
