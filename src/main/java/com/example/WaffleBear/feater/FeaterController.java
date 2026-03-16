package com.example.WaffleBear.feater;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.feater.model.FeaterDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
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

@RestController
@RequestMapping("/feater/settings")
@RequiredArgsConstructor
public class FeaterController {

    private final FeaterService featerService;

    @GetMapping("/me")
    public ResponseEntity<?> getSettings(@AuthenticationPrincipal AuthUserDetails userDetails) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.getSettings(userIdx);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateSettings(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody FeaterDto.SettingsUpdateReq request
    ) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.updateSettings(userIdx, request);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfileImage(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestParam("image") MultipartFile image
    ) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.uploadProfileImage(userIdx, image);

        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
