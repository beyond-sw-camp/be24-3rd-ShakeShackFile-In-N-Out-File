package com.example.WaffleBear.administrator;

import com.example.WaffleBear.administrator.model.AdministratorDto;
import com.example.WaffleBear.common.model.BaseResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "관리자 (Admin)", description = "관리자 전용 대시보드, 사용자 관리, 스토리지 분석 API")
@RestController
@RequestMapping("/administrator")
@RequiredArgsConstructor
public class AdministratorController {

    private final AdministratorService administratorService;
    private final StorageAnalyticsService storageAnalyticsService;

    @Operation(summary = "대시보드 조회", description = "관리자 대시보드 정보를 조회합니다. 사용자 요약, 플랜 통계, 전체 사용자 목록을 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대시보드 조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal AuthUserDetails userDetails) {
        AdministratorDto.DashboardRes result = administratorService.getDashboard(userDetails);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @Operation(summary = "사용자 상태 변경", description = "특정 사용자의 계정 상태를 변경합니다 (예: 활성, 정지, 차단).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 상태 변경 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/users/{userIdx}/status")
    public ResponseEntity<?> updateUserStatus(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "상태를 변경할 사용자 IDX", example = "1") @PathVariable Long userIdx,
            @RequestBody AdministratorDto.StatusUpdateReq request
    ) {
        AdministratorDto.UserRes result = administratorService.updateUserStatus(userDetails, userIdx, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @Operation(summary = "스토리지 분석 조회", description = "스토리지 사용 현황, 무결성 검사, 전송 내역 등 종합 분석 데이터를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스토리지 분석 조회 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다")
    })
    @GetMapping("/storage-analytics")
    public ResponseEntity<?> getStorageAnalytics(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "조회 기간 코드 (예: 7d, 30d, all)", example = "30d")
            @RequestParam(value = "range", required = false) String rangeCode
    ) {
        AdministratorDto.StorageAnalyticsRes result =
                storageAnalyticsService.getStorageAnalytics(userDetails, rangeCode);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @Operation(summary = "스토리지 용량 변경", description = "스토리지 제공자의 전체 용량을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스토리지 용량 변경 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 용량 값")
    })
    @PatchMapping("/storage-capacity")
    public ResponseEntity<?> updateStorageCapacity(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "조회 기간 코드 (예: 7d, 30d, all)", example = "30d")
            @RequestParam(value = "range", required = false) String rangeCode,
            @RequestBody AdministratorDto.StorageCapacityUpdateReq request
    ) {
        AdministratorDto.StorageAnalyticsRes result =
                storageAnalyticsService.updateProviderCapacity(userDetails, request, rangeCode);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
