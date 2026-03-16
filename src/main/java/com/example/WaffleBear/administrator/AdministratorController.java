package com.example.WaffleBear.administrator;

import com.example.WaffleBear.administrator.model.AdministratorDto;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administrator")
@RequiredArgsConstructor
public class AdministratorController {

    private final AdministratorService administratorService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal AuthUserDetails userDetails) {
        AdministratorDto.DashboardRes result = administratorService.getDashboard(userDetails);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/users/{userIdx}/status")
    public ResponseEntity<?> updateUserStatus(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long userIdx,
            @RequestBody AdministratorDto.StatusUpdateReq request
    ) {
        AdministratorDto.UserRes result = administratorService.updateUserStatus(userDetails, userIdx, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
