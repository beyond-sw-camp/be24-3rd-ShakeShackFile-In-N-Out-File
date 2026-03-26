package com.example.WaffleBear.file.lock;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.lock.dto.FileLockDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file/lock")
@Tag(name = "Drive Lock", description = "Drive file lock APIs")
@SecurityRequirement(name = "bearerAuth")
public class LockController {

    private final LockService lockService;

    @PatchMapping
    @Operation(summary = "Set locked state", description = "Locks or unlocks selected files.")
    public ResponseEntity<FileCommonDto.FileActionRes> setLockedFiles(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileLockDto.LockReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(lockService.setLockedFiles(
                userIdx,
                request != null ? request.getFileIdxList() : null,
                request != null && Boolean.TRUE.equals(request.getLocked())
        ));
    }
}
