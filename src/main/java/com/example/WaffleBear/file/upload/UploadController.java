package com.example.WaffleBear.file.upload;

import com.example.WaffleBear.file.upload.dto.UploadDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "파일 업로드 (Upload)", description = "파일 업로드 초기화, 완료, 중단 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/file/upload")
public class UploadController {

    private final UploadService uploadService;

    // file/upload의 기본 호출로 함
    @Operation(summary = "파일 업로드 초기화", description = "파일 업로드를 위한 사전 서명된 URL을 생성하고 업로드를 초기화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 초기화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<List<UploadDto.ChunkRes>> fileUpload(
            // 현재 로그인한 사용자 정보를 가져오는 라인
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            // 클라이언트에게 파일(들) 받기
            @Parameter(description = "업로드할 파일 정보 목록") @RequestBody List<UploadDto.InitReq> files
    ) {
        // 유저가 idx가 실제로 없으면 0을, 있으면 그 유저의 아이디
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        // 서비스 로직실행하고 프론트로 반환
        return ResponseEntity.ok(uploadService.init(userIdx, files));
    }

    // 업로드가 완료될 경우 실행하는 것인데, DB에 저장했다는 값을 적용 즉, 데이터 불일치를 없에기 위해 작업
    @Operation(summary = "파일 업로드 완료", description = "업로드가 완료된 파일의 청크를 병합하고 DB에 저장을 확정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 완료 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/complete")
    public ResponseEntity<UploadDto.CompleteRes> completeUpload(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "업로드 완료 요청 정보") @RequestBody UploadDto.CompleteReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(uploadService.complete(userIdx, request));
    }

    @Operation(summary = "파일 업로드 중단", description = "진행 중인 파일 업로드를 중단하고 임시 청크 파일을 정리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 중단 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/abort")
    public ResponseEntity<UploadDto.ActionRes> abortUpload(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails dto,
            @Parameter(description = "업로드 중단 요청 정보") @RequestBody UploadDto.AbortReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(uploadService.abort(userIdx, request));
    }
}
