package com.example.WaffleBear.file.manage;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "파일 관리 (FileManage)", description = "파일/폴더 목록 조회, 생성, 이동, 이름 변경, 휴지통, 영구 삭제 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileManageController {

    private final FileManageService fileManageService;

    @Operation(summary = "파일 목록 조회", description = "현재 사용자의 전체 파일 및 폴더 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/list")
    public ResponseEntity<List<FileCommonDto.FileListItemRes>> fileList(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto
    ) {
        // dto확인 즉, 로그인 상태 확인
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.list(userIdx));
    }

    @Operation(summary = "폴더 생성", description = "새로운 폴더를 생성합니다. 상위 폴더를 지정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "폴더 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/folder")
    public ResponseEntity<FileCommonDto.FileListItemRes> createFolder(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "폴더 생성 요청 정보") @RequestBody FileManageDto.FolderReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.createFolder(userIdx, request));
    }

    @Operation(summary = "파일/폴더 휴지통으로 이동", description = "지정된 파일 또는 폴더를 휴지통으로 이동합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "휴지통 이동 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @PatchMapping("/{fileIdx}/trash")
    public ResponseEntity<FileCommonDto.FileActionRes> moveToTrash(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "휴지통으로 이동할 파일/폴더 고유 ID", example = "1") @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveToTrash(userIdx, fileIdx));
    }

    @Operation(summary = "휴지통에서 복원", description = "휴지통에 있는 파일 또는 폴더를 원래 위치로 복원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복원 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @PatchMapping("/{fileIdx}/restore")
    public ResponseEntity<FileCommonDto.FileActionRes> restoreFromTrash(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "복원할 파일/폴더 고유 ID", example = "1") @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.restoreFromTrash(userIdx, fileIdx));
    }

    @Operation(summary = "파일/폴더 영구 삭제", description = "지정된 파일 또는 폴더를 영구적으로 삭제합니다. 복구가 불가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "영구 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @DeleteMapping("/{fileIdx}")
    public ResponseEntity<FileCommonDto.FileActionRes> deletePermanently(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "영구 삭제할 파일/폴더 고유 ID", example = "1") @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.deletePermanently(userIdx, fileIdx));
    }

    @Operation(summary = "휴지통 비우기", description = "현재 사용자의 휴지통에 있는 모든 파일과 폴더를 영구적으로 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "휴지통 비우기 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/trash")
    public ResponseEntity<FileCommonDto.FileActionRes> clearTrash(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.clearTrash(userIdx));
    }

    @Operation(summary = "파일/폴더 이동", description = "지정된 파일 또는 폴더를 다른 폴더로 이동합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이동 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @PatchMapping("/{fileIdx}/move")
    public ResponseEntity<FileCommonDto.FileActionRes> moveToFolder(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "이동할 파일/폴더 고유 ID", example = "1") @PathVariable Long fileIdx,
            @Parameter(description = "이동 대상 정보") @RequestBody FileManageDto.MoveReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveToFolder(
                userIdx,
                fileIdx,
                request != null ? request.getTargetParentId() : null
        ));
    }

    @Operation(summary = "파일/폴더 이름 변경", description = "지정된 파일 또는 폴더의 이름을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이름 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @PatchMapping("/{fileIdx}/rename")
    public ResponseEntity<FileCommonDto.FileListItemRes> renameFolder(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "이름을 변경할 파일/폴더 고유 ID", example = "1") @PathVariable Long fileIdx,
            @Parameter(description = "새 이름 정보") @RequestBody FileManageDto.RenameReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.renameFolder(
                userIdx,
                fileIdx,
                request != null ? request.getFileName() : null
        ));
    }

    @Operation(summary = "여러 파일 일괄 이동", description = "선택한 여러 파일/폴더를 지정된 대상 폴더로 일괄 이동합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일괄 이동 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/move")
    public ResponseEntity<FileCommonDto.FileActionRes> moveFilesToFolder(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "일괄 이동 요청 정보") @RequestBody FileManageDto.MoveBatchReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveFilesToFolder(
                userIdx,
                request != null ? request.getFileIdxList() : null,
                request != null ? request.getTargetParentId() : null
        ));
    }

    @Operation(summary = "여러 파일 일괄 복원", description = "선택한 여러 파일/폴더를 휴지통에서 일괄 복원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일괄 복원 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/restore")
    public ResponseEntity<FileCommonDto.FileActionRes> restoreFilesFromTrash(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "일괄 복원 요청 정보") @RequestBody FileManageDto.RestoreBatchReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.restoreFilesFromTrash(
                userIdx,
                request != null ? request.getFileIdxList() : null
        ));
    }
}
