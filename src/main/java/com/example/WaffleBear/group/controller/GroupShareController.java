package com.example.WaffleBear.group.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.group.model.dto.GroupShareDto;
import com.example.WaffleBear.group.service.GroupShareService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group/share")
@Tag(name = "Group Share", description = "Group-based sharing APIs")
@SecurityRequirement(name = "bearerAuth")
public class GroupShareController {

    private final GroupShareService groupShareService;

    @GetMapping("/chats/overview")
    @Operation(summary = "Get chat share overview", description = "Returns overview data for group-based chat sharing.")
    public BaseResponse<?> getChatShareOverview(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return BaseResponse.success(groupShareService.getChatShareOverview(user.getIdx()));
    }

    @PostMapping("/files")
    @Operation(summary = "Share files to group", description = "Shares drive files with a group.")
    public BaseResponse<?> shareFiles(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.FileShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareFiles(user.getIdx(), request));
    }

    @PostMapping("/workspaces")
    @Operation(summary = "Share workspaces to group", description = "Shares workspaces with a group.")
    public BaseResponse<?> shareWorkspaces(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.WorkspaceShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareWorkspace(user.getIdx(), request));
    }

    @PostMapping("/chats")
    @Operation(summary = "Share chats to group", description = "Adds group members into a chat room.")
    public BaseResponse<?> shareChats(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.ChatShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareChat(user.getIdx(), request));
    }
}
