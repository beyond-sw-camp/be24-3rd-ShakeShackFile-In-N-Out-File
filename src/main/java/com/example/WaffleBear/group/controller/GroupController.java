package com.example.WaffleBear.group.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.group.model.dto.GroupRelationshipDto;
import com.example.WaffleBear.group.model.dto.RelationshipGroupDto;
import com.example.WaffleBear.group.service.GroupRelationshipService;
import com.example.WaffleBear.group.service.RelationshipGroupService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "그룹 & 관계 (Group)", description = "친구 관계 및 그룹 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {

    private final GroupRelationshipService groupRelationshipService;
    private final RelationshipGroupService relationshipGroupService;

    @Operation(summary = "관계 목록 조회", description = "현재 사용자의 모든 관계(친구) 목록과 받은/보낸 초대를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관계 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/relationships")
    public BaseResponse<?> getRelationships(@AuthenticationPrincipal AuthUserDetails user) {
        return BaseResponse.success(groupRelationshipService.getRelationshipList(user.getIdx()));
    }

    @Operation(summary = "초대 생성", description = "다른 사용자에게 친구 초대를 보냅니다. 사용자 ID 또는 이메일로 초대할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 관계가 존재하거나 자기 자신에게 초대)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/invites")
    public BaseResponse<?> createInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody GroupRelationshipDto.CreateInviteRequest request
    ) {
        return BaseResponse.success(groupRelationshipService.createInvite(user.getIdx(), request));
    }

    @Operation(summary = "초대 수락", description = "받은 친구 초대를 수락하여 관계를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 수락 성공"),
            @ApiResponse(responseCode = "404", description = "초대를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/invites/{inviteId}/accept")
    public BaseResponse<?> acceptInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "수락할 초대 ID", example = "1") @PathVariable Long inviteId
    ) {
        return BaseResponse.success(groupRelationshipService.acceptInvite(user.getIdx(), inviteId));
    }

    @Operation(summary = "초대 거절", description = "받은 친구 초대를 거절합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 거절 성공"),
            @ApiResponse(responseCode = "404", description = "초대를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/invites/{inviteId}/reject")
    public BaseResponse<?> rejectInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "거절할 초대 ID", example = "1") @PathVariable Long inviteId
    ) {
        return BaseResponse.success(groupRelationshipService.rejectInvite(user.getIdx(), inviteId));
    }

    @Operation(summary = "관계 상태 변경", description = "기존 관계의 상태를 변경합니다 (예: 차단, 즐겨찾기 등).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관계 상태 변경 성공"),
            @ApiResponse(responseCode = "404", description = "관계를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/relationships/{relationshipId}/status")
    public BaseResponse<?> updateRelationshipStatus(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "관계 ID", example = "1") @PathVariable Long relationshipId,
            @RequestBody GroupRelationshipDto.UpdateRelationshipStatusRequest request
    ) {
        return BaseResponse.success(groupRelationshipService.updateRelationshipStatus(user.getIdx(), relationshipId, request));
    }

    @Operation(summary = "관계 삭제", description = "기존 관계(친구)를 삭제하여 연결을 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관계 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "관계를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @DeleteMapping("/relationships/{relationshipId}")
    public BaseResponse<?> deleteRelationship(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "삭제할 관계 ID", example = "1") @PathVariable Long relationshipId
    ) {
        groupRelationshipService.deleteRelationship(user.getIdx(), relationshipId);
        return BaseResponse.success("연결을 해제했습니다.");
    }

    @Operation(summary = "그룹 개요 조회", description = "사용자의 모든 그룹, 그룹 상세, 미분류 관계, 그룹 초대 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 개요 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/overview")
    public BaseResponse<?> getOverview(@AuthenticationPrincipal AuthUserDetails user) {
        return BaseResponse.success(relationshipGroupService.getOverview(user.getIdx()));
    }

    @Operation(summary = "그룹 생성", description = "새로운 연락처 그룹을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 그룹명 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/groups")
    public BaseResponse<?> createGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody RelationshipGroupDto.CreateGroupRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.createGroup(user.getIdx(), request));
    }

    @Operation(summary = "그룹 이름 변경", description = "기존 그룹의 이름을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 이름 변경 성공"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/groups/{groupId}")
    public BaseResponse<?> renameGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "그룹 ID", example = "1") @PathVariable Long groupId,
            @RequestBody RelationshipGroupDto.UpdateGroupRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.renameGroup(user.getIdx(), groupId, request));
    }

    @Operation(summary = "그룹 삭제", description = "기존 그룹을 삭제합니다. 그룹에 속한 관계는 미분류로 이동됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @DeleteMapping("/groups/{groupId}")
    public BaseResponse<?> deleteGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "삭제할 그룹 ID", example = "1") @PathVariable Long groupId
    ) {
        relationshipGroupService.deleteGroup(user.getIdx(), groupId);
        return BaseResponse.success("그룹이 삭제되었습니다.");
    }

    @Operation(summary = "관계를 그룹에 추가", description = "특정 관계(친구)를 지정한 그룹에 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 추가 성공"),
            @ApiResponse(responseCode = "404", description = "관계 또는 그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/relationships/{relationshipId}/groups")
    public BaseResponse<?> addRelationshipToGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "관계 ID", example = "1") @PathVariable Long relationshipId,
            @RequestBody RelationshipGroupDto.AddGroupMappingRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.addRelationshipToGroup(user.getIdx(), relationshipId, request));
    }

    @Operation(summary = "관계의 그룹 전체 교체", description = "특정 관계가 속한 그룹 목록을 전체 교체합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 교체 성공"),
            @ApiResponse(responseCode = "404", description = "관계를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PutMapping("/relationships/{relationshipId}/groups")
    public BaseResponse<?> replaceRelationshipGroups(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "관계 ID", example = "1") @PathVariable Long relationshipId,
            @RequestBody RelationshipGroupDto.ReplaceGroupMappingsRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.replaceRelationshipGroups(user.getIdx(), relationshipId, request));
    }

    @Operation(summary = "관계를 그룹에서 제거", description = "특정 관계를 지정한 그룹에서 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹에서 제거 성공"),
            @ApiResponse(responseCode = "404", description = "관계 또는 그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @DeleteMapping("/relationships/{relationshipId}/groups/{groupId}")
    public BaseResponse<?> removeRelationshipFromGroup(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "관계 ID", example = "1") @PathVariable Long relationshipId,
            @Parameter(description = "그룹 ID", example = "1") @PathVariable Long groupId
    ) {
        return BaseResponse.success(relationshipGroupService.removeRelationshipFromGroup(user.getIdx(), relationshipId, groupId));
    }

    @Operation(summary = "그룹 초대 생성", description = "다른 사용자를 특정 그룹에 초대합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 초대 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/group-invites")
    public BaseResponse<?> createGroupInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody RelationshipGroupDto.CreateGroupInviteRequest request
    ) {
        return BaseResponse.success(relationshipGroupService.createGroupInvite(user.getIdx(), request));
    }

    @Operation(summary = "그룹 초대 수락", description = "받은 그룹 초대를 수락합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 초대 수락 성공"),
            @ApiResponse(responseCode = "404", description = "초대를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/group-invites/{inviteId}/accept")
    public BaseResponse<?> acceptGroupInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "수락할 그룹 초대 ID", example = "1") @PathVariable Long inviteId
    ) {
        return BaseResponse.success(relationshipGroupService.acceptGroupInvite(user.getIdx(), inviteId));
    }

    @Operation(summary = "그룹 초대 거절", description = "받은 그룹 초대를 거절합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "그룹 초대 거절 성공"),
            @ApiResponse(responseCode = "404", description = "초대를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/group-invites/{inviteId}/reject")
    public BaseResponse<?> rejectGroupInvite(
            @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "거절할 그룹 초대 ID", example = "1") @PathVariable Long inviteId
    ) {
        return BaseResponse.success(relationshipGroupService.rejectGroupInvite(user.getIdx(), inviteId));
    }
}
