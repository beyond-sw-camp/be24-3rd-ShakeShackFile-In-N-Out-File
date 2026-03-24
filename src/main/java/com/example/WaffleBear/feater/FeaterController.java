package com.example.WaffleBear.feater;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.feater.model.FeaterDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자 설정 (Feater)", description = "사용자 설정 조회, 수정, 프로필 이미지 업로드 API")
@RestController
@RequestMapping("/feater/settings")
@RequiredArgsConstructor
public class FeaterController {

    private final FeaterService featerService;

    @Operation(summary = "내 설정 조회", description = "로그인한 사용자의 설정 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.getSettings(userIdx);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @Operation(summary = "내 설정 수정", description = "로그인한 사용자의 설정 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PutMapping("/me")
    public ResponseEntity<?> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody FeaterDto.SettingsUpdateReq request
    ) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.updateSettings(userIdx, request);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @Operation(summary = "프로필 이미지 업로드", description = "프로필 이미지를 업로드하여 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 파일")
    })
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "프로필 이미지 파일") @RequestParam("image") MultipartFile image
    ) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.uploadProfileImage(userIdx, image);

        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
