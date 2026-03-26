package com.example.WaffleBear.administrator;

import com.example.WaffleBear.administrator.model.AdministratorDto;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/administrator")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrator dashboard and storage analytics APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdministratorController {

    private final AdministratorService administratorService;
    private final StorageAnalyticsService storageAnalyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard", description = "Returns administrator dashboard data.")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal AuthUserDetails userDetails) {
        AdministratorDto.DashboardRes result = administratorService.getDashboard(userDetails);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/users/{userIdx}/status")
    @Operation(summary = "Update user status", description = "Changes a user's account status.")
    public ResponseEntity<?> updateUserStatus(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long userIdx,
            @RequestBody AdministratorDto.StatusUpdateReq request
    ) {
        AdministratorDto.UserRes result = administratorService.updateUserStatus(userDetails, userIdx, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/storage-analytics")
    @Operation(summary = "Get storage analytics", description = "Returns storage and transfer analytics for the selected range.")
    public ResponseEntity<?> getStorageAnalytics(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestParam(value = "range", required = false) String rangeCode
    ) {
        AdministratorDto.StorageAnalyticsRes result =
                storageAnalyticsService.getStorageAnalytics(userDetails, rangeCode);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/storage-capacity")
    @Operation(summary = "Update storage capacity", description = "Updates the configured provider capacity for analytics.")
    public ResponseEntity<?> updateStorageCapacity(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestParam(value = "range", required = false) String rangeCode,
            @RequestBody AdministratorDto.StorageCapacityUpdateReq request
    ) {
        AdministratorDto.StorageAnalyticsRes result =
                storageAnalyticsService.updateProviderCapacity(userDetails, request, rangeCode);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
