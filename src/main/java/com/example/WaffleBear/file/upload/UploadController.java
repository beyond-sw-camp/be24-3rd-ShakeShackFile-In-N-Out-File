package com.example.WaffleBear.file.upload;

import com.example.WaffleBear.file.upload.dto.UploadDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file/upload")
public class UploadController {

    private final UploadService uploadService;

    // file/upload의 기본 호출로 함
    @PostMapping
    public ResponseEntity<List<UploadDto.ChunkRes>> fileUpload(
            // 현재 로그인한 사용자 정보를 가져오는 라인
            @AuthenticationPrincipal AuthUserDetails dto,
            // 클라이언트에게 파일(들) 받기
            @RequestBody List<UploadDto.InitReq> files
    ) {
        // 유저가 idx가 실제로 없으면 0을, 있으면 그 유저의 아이디
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        // 서비스 로직실행하고 프론트로 반환
        return ResponseEntity.ok(uploadService.init(userIdx, files));
    }

    @PostMapping("/complete")
    public ResponseEntity<UploadDto.CompleteRes> completeUpload(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody UploadDto.CompleteReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(uploadService.complete(userIdx, request));
    }

    @PostMapping("/abort")
    public ResponseEntity<UploadDto.ActionRes> abortUpload(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody UploadDto.AbortReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(uploadService.abort(userIdx, request));
    }
}
