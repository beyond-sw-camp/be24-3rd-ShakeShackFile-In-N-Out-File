package com.example.WaffleBear.group.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.group.model.dto.GroupRelationshipDto;
import com.example.WaffleBear.group.model.dto.RelationshipGroupDto;
import com.example.WaffleBear.group.service.GroupRelationshipService;
import com.example.WaffleBear.group.service.RelationshipGroupService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {

    private final GroupRelationshipService groupRelationshipService;
    private final RelationshipGroupService relationshipGroupService;

    @GetMapping("/relationships")
    public BaseResponse<?> getRelationships(@AuthenticationPrincipal AuthUserDetails user) {
        return BaseResponse.success(groupRelationshipService.getRelationshipList(user.getIdx()));
    }

    @PostMapping("/invites")
    public BaseResponse<?> createInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupRelationshipDto.CreateInviteRequest request
    ) {
        return BaseResponse.success(groupRelationshipService.createInvite(user.getIdx(), request));
    }

    @PatchMapping("/invites/{inviteId}/accept")
    public BaseResponse<?> acceptInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long inviteId
    ) {
        return BaseResponse.success(groupRelationshipService.acceptInvite(user.getIdx(), inviteId));
    }

    @PatchMapping("/invites/{inviteId}/reject")
    public BaseResponse<?> rejectInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long inviteId
    ) {
        return BaseResponse.success(groupRelationshipService.rejectInvite(user.getIdx(), inviteId));
    }

    @PatchMapping("/relationships/{relationshipId}/status")
    public BaseResponse<?> updateRelationshipStatus(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long relationshipId,
            @RequestBody GroupRelationshipDto.UpdateRelationshipStatusRequest request
    ) {
        return BaseResponse.success(groupRelationshipService.updateRelationshipStatus(user.getIdx(), relationshipId, request));
    }

    @DeleteMapping("/relationships/{relationshipId}")
    public BaseResponse<?> deleteRelationship(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long relationshipId
    ) {
        groupRelationshipService.deleteRelationship(user.getIdx(), relationshipId);
        return BaseResponse.success("연결을 해제했습니다.");
    }

    @GetMapping("/overview")
    public BaseResponse<?> getOverview(@AuthenticationPrincipal AuthUserDetails user) {
        return BaseResponse.success(relationshipGroupService.getOverview(user.getIdx()));
    }

    @PostMapping("/groups")
    public BaseResponse<?> createGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody RelationshipGroupDto.CreateGroupRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.createGroup(user.getIdx(), request));
    }

    @PatchMapping("/groups/{groupId}")
    public BaseResponse<?> renameGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long groupId,
            @RequestBody RelationshipGroupDto.UpdateGroupRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.renameGroup(user.getIdx(), groupId, request));
    }

    @DeleteMapping("/groups/{groupId}")
    public BaseResponse<?> deleteGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long groupId
    ) {
        relationshipGroupService.deleteGroup(user.getIdx(), groupId);
        return BaseResponse.success("그룹이 삭제되었습니다.");
    }

    @PostMapping("/relationships/{relationshipId}/groups")
    public BaseResponse<?> addRelationshipToGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long relationshipId,
            @RequestBody RelationshipGroupDto.AddGroupMappingRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.addRelationshipToGroup(user.getIdx(), relationshipId, request));
    }

    @PutMapping("/relationships/{relationshipId}/groups")
    public BaseResponse<?> replaceRelationshipGroups(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long relationshipId,
            @RequestBody RelationshipGroupDto.ReplaceGroupMappingsRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.replaceRelationshipGroups(user.getIdx(), relationshipId, request));
    }

    @DeleteMapping("/relationships/{relationshipId}/groups/{groupId}")
    public BaseResponse<?> removeRelationshipFromGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long relationshipId,
            @PathVariable Long groupId
    ) {
        return BaseResponse.success(relationshipGroupService.removeRelationshipFromGroup(user.getIdx(), relationshipId, groupId));
    }

    @PostMapping("/group-invites")
    public BaseResponse<?> createGroupInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody RelationshipGroupDto.CreateGroupInviteRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.createGroupInvite(user.getIdx(), request));
    }

    @PatchMapping("/group-invites/{inviteId}/accept")
    public BaseResponse<?> acceptGroupInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long inviteId
    ) {
        return BaseResponse.success(relationshipGroupService.acceptGroupInvite(user.getIdx(), inviteId));
    }

    @PatchMapping("/group-invites/{inviteId}/reject")
    public BaseResponse<?> rejectGroupInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long inviteId
    ) {
        return BaseResponse.success(relationshipGroupService.rejectGroupInvite(user.getIdx(), inviteId));
    }
}
