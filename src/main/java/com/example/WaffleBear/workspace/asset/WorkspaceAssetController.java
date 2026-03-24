package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "워크스페이스 에셋 (WorkspaceAsset)", description = "워크스페이스 첨부파일 업로드, 조회, 삭제, 드라이브 저장 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/workspace")
public class WorkspaceAssetController {

    private final WorkspaceAssetService workspaceAssetService;

    @Operation(summary = "에셋 목록 조회", description = "워크스페이스에 첨부된 에셋(파일) 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping("/{workspaceId}/assets")
    public ResponseEntity<List<WorkspaceAssetDto.AssetRes>> listAssets(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId
    ) {
        return ResponseEntity.ok(
                workspaceAssetService.listAssets(
                        user != null ? user.getIdx() : 0L,
                        workspaceId
                )
        );
    }

    @Operation(summary = "에셋 업로드", description = "워크스페이스에 여러 파일을 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일")
    })
    @PostMapping("/{workspaceId}/assets")
    public BaseResponse<?> uploadWorkspaceAssets(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId,
            @Parameter(description = "업로드할 파일들") @RequestParam("files") MultipartFile[] files
    ) {
        List<WorkspaceAssetDto.AssetRes> result = workspaceAssetService.uploadWorkspaceAssets(
                user.getIdx(),
                workspaceId,
                files
        );
        return BaseResponse.success(result);
    }

    @Operation(summary = "EditorJS 이미지 업로드", description = "EditorJS 에디터에서 사용할 이미지를 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 파일")
    })
    @PostMapping(value = "/{workspaceId}/assets/editorjs",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity uploadEditorJsImage(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId,
            @Parameter(description = "업로드할 이미지 파일") @RequestParam("image") MultipartFile image
    ) {
        WorkspaceAssetService.EditorJsUploadResult
                file = workspaceAssetService.uploadAssetsEditorJs(user.getIdx(),workspaceId, image);

        return ResponseEntity.ok(Map.of(
                "success", 1,
                "file", Map.of("url", file.fileUrl(),
                        "assetIdx", file.assetIdx())));
    }

    @Operation(summary = "EditorJS 이미지 삭제", description = "EditorJS 에디터에서 업로드된 이미지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "에셋을 찾을 수 없음")
    })
    @DeleteMapping(value = "/{workspaceId}/assets/{assetIdx}/editorjs",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteEditorJsImage(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId,
            @Parameter(description = "에셋 ID", example = "5") @PathVariable Long assetIdx
    ) {
        workspaceAssetService.deleteEditorJsImage(user.getIdx(), workspaceId, assetIdx);
        return ResponseEntity.ok(Map.of("success", 1));
    }

    @Operation(summary = "에셋 삭제", description = "워크스페이스에서 에셋을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "에셋을 찾을 수 없음")
    })
    @DeleteMapping("/{workspaceId}/assets/{assetId}")
    public ResponseEntity<?> deleteWorkspaceAsset(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId,
            @Parameter(description = "에셋 ID", example = "5") @PathVariable Long assetId
    ) {
        workspaceAssetService.deleteWorkspaceAsset(user.getIdx(), workspaceId, assetId);
        return ResponseEntity.ok(Map.of("success", 1));
    }

    @Operation(summary = "에셋을 내 드라이브에 저장", description = "워크스페이스 에셋을 내 드라이브에 복사하여 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "드라이브 저장 성공"),
            @ApiResponse(responseCode = "404", description = "에셋을 찾을 수 없음")
    })
    @PostMapping("/{workspaceId}/assets/{assetId}/save-to-drive")
    public ResponseEntity<FileCommonDto.FileListItemRes> saveAssetToDrive(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long workspaceId,
            @Parameter(description = "에셋 ID", example = "5") @PathVariable Long assetId,
            @Parameter(description = "저장할 대상 폴더 ID (선택)") @RequestParam(required = false) Long parentId
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
