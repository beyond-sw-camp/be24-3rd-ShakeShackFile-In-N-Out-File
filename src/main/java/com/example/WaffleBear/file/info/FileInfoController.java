package com.example.WaffleBear.file.info;

import com.example.WaffleBear.file.info.dto.FileInfoDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "파일 정보 (FileInfo)", description = "파일/폴더 속성 조회, 저장소 요약, 텍스트 미리보기 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileInfoController {

    private final FileInfoService fileInfoService;

    @Operation(summary = "폴더 속성 조회", description = "지정된 폴더의 하위 파일/폴더 개수, 용량 등 상세 속성을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "폴더 속성 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @GetMapping("/{fileIdx}/properties")
    public ResponseEntity<FileInfoDto.FolderPropertyRes> getFolderProperties(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "조회할 파일/폴더 고유 ID", example = "1") @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getFolderProperties(userIdx, fileIdx));
    }

    @Operation(summary = "저장소 사용량 요약 조회", description = "현재 사용자의 저장소 사용량, 요금제, 카테고리별 사용 현황을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장소 요약 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/storage/summary")
    public ResponseEntity<FileInfoDto.StorageSummaryRes> getStorageSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getStorageSummary(userIdx));
    }

    @Operation(summary = "텍스트 파일 미리보기", description = "텍스트 기반 파일의 내용을 미리보기로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "텍스트 미리보기 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @GetMapping("/{fileIdx}/text-preview")
    public ResponseEntity<FileInfoDto.TextPreviewRes> getTextPreview(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "미리보기할 파일 고유 ID", example = "1") @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getTextPreview(userIdx, fileIdx));
    }
}
