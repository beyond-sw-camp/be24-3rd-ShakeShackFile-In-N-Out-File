package com.example.WaffleBear.file.share;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.share.model.ShareDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "파일 공유 (Share)", description = "파일 공유, 공유 취소, 공유 파일 저장, 공유 목록 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/file/share")
public class ShareController {

    private final ShareService shareService;

    @Operation(summary = "나에게 공유된 파일 목록 조회", description = "다른 사용자가 나에게 공유한 파일 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유받은 파일 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/shared/list")
    public ResponseEntity<List<ShareDto.SharedFileRes>> sharedFileList(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(shareService.sharedFileList(userDetails != null ? userDetails.getIdx() : 0L));
    }

    @Operation(summary = "내가 공유한 파일 목록 조회", description = "내가 다른 사용자에게 공유한 파일 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유한 파일 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/sent/list")
    public ResponseEntity<List<ShareDto.SentSharedFileRes>> sentSharedFileList(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(shareService.sentSharedFileList(userDetails != null ? userDetails.getIdx() : 0L));
    }

    @Operation(summary = "파일 공유 정보 조회", description = "특정 파일의 공유 상태 및 공유 대상 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @GetMapping("/{fileIdx}")
    public ResponseEntity<List<ShareDto.ShareInfoRes>> getShareInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "공유 정보를 조회할 파일 고유 ID", example = "1") @PathVariable Long fileIdx) {
        return ResponseEntity.ok(shareService.getShareInfo(userDetails != null ? userDetails.getIdx() : 0L, fileIdx));
    }

    @Operation(summary = "파일 공유", description = "선택한 파일을 지정된 이메일 주소의 사용자에게 공유합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 공유 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<FileCommonDto.FileActionRes> shareFiles(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "파일 공유 요청 정보") @RequestBody ShareDto.ShareReq request) {
        return ResponseEntity.ok(shareService.shareFiles(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null,
                request != null ? request.recipientEmail() : null
        ));
    }

    @Operation(summary = "파일 공유 취소", description = "선택한 파일의 특정 사용자에 대한 공유를 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/cancel")
    public ResponseEntity<FileCommonDto.FileActionRes> cancelShare(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "공유 취소 요청 정보") @RequestBody ShareDto.ShareReq request) {
        return ResponseEntity.ok(shareService.cancelShare(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null,
                request != null ? request.recipientEmail() : null
        ));
    }

    @Operation(summary = "파일 전체 공유 취소", description = "선택한 파일의 모든 공유를 일괄 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 공유 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/cancel-all")
    public ResponseEntity<FileCommonDto.FileActionRes> cancelAllShares(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "전체 공유 취소 요청 정보") @RequestBody ShareDto.CancelAllShareReq request) {
        return ResponseEntity.ok(shareService.cancelAllShares(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null
        ));
    }

    @Operation(summary = "공유 파일을 내 드라이브에 저장", description = "나에게 공유된 파일을 내 드라이브의 지정된 폴더에 복사하여 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "드라이브 저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @PostMapping("/shared/{fileIdx}/save")
    public ResponseEntity<FileCommonDto.FileListItemRes> saveSharedFileToDrive(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "저장할 공유 파일 고유 ID", example = "1") @PathVariable Long fileIdx,
            @Parameter(description = "저장 대상 폴더 정보 (선택사항)") @RequestBody(required = false) ShareDto.SaveToDriveReq request) {
        return ResponseEntity.ok(shareService.saveSharedFileToDrive(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx,
                request != null ? request.parentId() : null
        ));
    }

    @Operation(summary = "공유 파일 텍스트 미리보기", description = "나에게 공유된 텍스트 파일의 내용을 미리보기로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "텍스트 미리보기 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @GetMapping("/shared/{fileIdx}/text-preview")
    public ResponseEntity<FileInfoDto.TextPreviewRes> getSharedTextPreview(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "미리보기할 공유 파일 고유 ID", example = "1") @PathVariable Long fileIdx) {
        return ResponseEntity.ok(shareService.getSharedTextPreview(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx
        ));
    }
}

