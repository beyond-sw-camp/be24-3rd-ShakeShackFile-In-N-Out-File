package com.example.WaffleBear.group.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.group.model.dto.GroupShareDto;
import com.example.WaffleBear.group.service.GroupShareService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "그룹 공유 (GroupShare)", description = "그룹 기반 파일, 워크스페이스, 채팅 공유 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/group/share")
public class GroupShareController {

    private final GroupShareService groupShareService;

    @Operation(summary = "채팅 공유 개요 조회", description = "현재 사용자의 채팅 공유 가능한 그룹 및 관계 개요를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅 공유 개요 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/chats/overview")
    public BaseResponse<?> getChatShareOverview(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return BaseResponse.success(groupShareService.getChatShareOverview(user.getIdx()));
    }

    @Operation(summary = "파일 공유", description = "선택한 파일들을 사용자, 그룹, 이메일 수신자에게 공유합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 공유 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 ID 누락 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/files")
    public BaseResponse<?> shareFiles(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.FileShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareFiles(user.getIdx(), request));
    }

    @Operation(summary = "워크스페이스 공유", description = "워크스페이스를 사용자, 그룹, 이메일 수신자에게 공유합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "워크스페이스 공유 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (워크스페이스 ID 누락 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/workspaces")
    public BaseResponse<?> shareWorkspaces(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.WorkspaceShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareWorkspace(user.getIdx(), request));
    }

    @Operation(summary = "채팅 공유", description = "채팅방을 사용자, 그룹, 이메일 수신자에게 공유합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅 공유 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (채팅방 ID 누락 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/chats")
    public BaseResponse<?> shareChats(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.ChatShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareChat(user.getIdx(), request));
    }
}
