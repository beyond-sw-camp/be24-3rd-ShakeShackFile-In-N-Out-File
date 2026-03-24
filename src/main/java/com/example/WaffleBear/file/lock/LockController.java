package com.example.WaffleBear.file.lock;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.lock.dto.FileLockDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "파일 잠금 (Lock)", description = "파일 잠금/잠금 해제 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/file/lock")
public class LockController {

    private final LockService lockService;

    @Operation(summary = "파일 잠금 설정/해제", description = "선택한 파일들의 잠금 상태를 설정하거나 해제합니다. 잠금된 파일은 수정 및 삭제가 제한됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 잠금 상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping
    public ResponseEntity<FileCommonDto.FileActionRes> setLockedFiles(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "잠금 설정 요청 정보") @RequestBody FileLockDto.LockReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(lockService.setLockedFiles(
                userIdx,
                request != null ? request.getFileIdxList() : null,
                request != null && Boolean.TRUE.equals(request.getLocked())
        ));
    }
}
