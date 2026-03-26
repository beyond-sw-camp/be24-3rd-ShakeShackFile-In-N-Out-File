package com.example.WaffleBear.feater;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.feater.model.FeaterDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@RestController
@RequestMapping("/feater/settings")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profile and personal settings APIs")
@SecurityRequirement(name = "bearerAuth")
public class FeaterController {

    private final FeaterService featerService;

    @GetMapping("/me")
    @Operation(summary = "Get my settings", description = "Returns the current user's profile and settings.")
    public ResponseEntity<?> getSettings(@AuthenticationPrincipal AuthUserDetails userDetails) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.getSettings(userIdx);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my settings", description = "Updates profile settings and user preferences.")
    public ResponseEntity<?> updateSettings(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody FeaterDto.SettingsUpdateReq request
    ) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.updateSettings(userIdx, request);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile image", description = "Uploads or replaces the current user's profile image.")
    public ResponseEntity<?> uploadProfileImage(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestParam("image") MultipartFile image
    ) {
        Long userIdx = userDetails != null ? userDetails.getIdx() : null;
        FeaterDto.SettingsRes result = featerService.uploadProfileImage(userIdx, image);

        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
