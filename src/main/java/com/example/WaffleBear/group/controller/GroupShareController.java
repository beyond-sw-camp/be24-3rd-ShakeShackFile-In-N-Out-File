package com.example.WaffleBear.group.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.group.model.dto.GroupShareDto;
import com.example.WaffleBear.group.service.GroupShareService;
import com.example.WaffleBear.user.model.AuthUserDetails;
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
public class GroupShareController {

    private final GroupShareService groupShareService;

    @GetMapping("/chats/overview")
    public BaseResponse<?> getChatShareOverview(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return BaseResponse.success(groupShareService.getChatShareOverview(user.getIdx()));
    }

    @PostMapping("/files")
    public BaseResponse<?> shareFiles(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.FileShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareFiles(user.getIdx(), request));
    }

    @PostMapping("/workspaces")
    public BaseResponse<?> shareWorkspaces(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.WorkspaceShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareWorkspace(user.getIdx(), request));
    }

    @PostMapping("/chats")
    public BaseResponse<?> shareChats(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupShareDto.ChatShareRequest request
    ) {
        return BaseResponse.success(groupShareService.shareChat(user.getIdx(), request));
    }
}
