package com.example.WaffleBear.group.model.dto;

import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import com.example.WaffleBear.group.model.entity.RelationshipGroupInvite;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class RelationshipGroupDto {

    @Schema(description = "그룹 생성 요청")
    public record CreateGroupRequest(
            @Schema(description = "그룹명", example = "개발팀")
            String name
    ) {
    }

    @Schema(description = "그룹 이름 변경 요청")
    public record UpdateGroupRequest(
            @Schema(description = "변경할 그룹명", example = "디자인팀")
            String name
    ) {
    }

    @Schema(description = "그룹 매핑 추가 요청")
    public record AddGroupMappingRequest(
            @Schema(description = "추가할 그룹 ID", example = "1")
            Long groupId
    ) {
    }

    @Schema(description = "그룹 매핑 일괄 변경 요청")
    public record ReplaceGroupMappingsRequest(
            @Schema(description = "설정할 그룹 ID 목록", example = "[1, 2, 3]")
            List<Long> groupIds
    ) {
    }

    @Schema(description = "그룹 초대 생성 요청")
    public record CreateGroupInviteRequest(
            @Schema(description = "그룹 ID", example = "1")
            Long groupId,
            @Schema(description = "초대할 사용자 IDX", example = "5")
            Long toUserId
    ) {
    }

    @Schema(description = "그룹 요약 정보")
    public record GroupSummary(
            @Schema(description = "그룹 ID", example = "1")
            Long groupId,
            @Schema(description = "그룹명", example = "개발팀")
            String name,
            @Schema(description = "그룹 생성 일시")
            LocalDateTime createdAt,
            @Schema(description = "그룹 내 관계 수", example = "5")
            long relationshipCount
    ) {
        public static GroupSummary from(RelationshipGroup group, long relationshipCount) {
            return new GroupSummary(
                    group.getId(),
                    group.getName(),
                    group.getCreatedAt(),
                    relationshipCount
            );
        }
    }

    @Schema(description = "그룹 상세 정보")
    public record GroupDetail(
            @Schema(description = "그룹 ID", example = "1")
            Long groupId,
            @Schema(description = "그룹명", example = "개발팀")
            String name,
            @Schema(description = "그룹 생성 일시")
            LocalDateTime createdAt,
            @Schema(description = "그룹 내 관계 목록")
            List<GroupRelationshipDto.RelationshipSummary> relationships
    ) {
        public static GroupDetail from(RelationshipGroup group, List<GroupRelationshipDto.RelationshipSummary> relationships) {
            return new GroupDetail(
                    group.getId(),
                    group.getName(),
                    group.getCreatedAt(),
                    relationships
            );
        }
    }

    @Schema(description = "그룹 초대 요약 정보")
    public record GroupInviteSummary(
            @Schema(description = "그룹 초대 ID", example = "1")
            Long groupInviteId,
            @Schema(description = "그룹 ID", example = "1")
            Long groupId,
            @Schema(description = "그룹명", example = "개발팀")
            String groupName,
            @Schema(description = "초대 보낸 사용자")
            GroupRelationshipDto.UserSummary fromUser,
            @Schema(description = "초대 받은 사용자")
            GroupRelationshipDto.UserSummary toUser,
            @Schema(description = "초대 상태", example = "PENDING")
            String status,
            @Schema(description = "초대 생성 일시")
            LocalDateTime createdAt
    ) {
        public static GroupInviteSummary from(RelationshipGroupInvite invite) {
            return new GroupInviteSummary(
                    invite.getId(),
                    invite.getGroup().getId(),
                    invite.getGroup().getName(),
                    GroupRelationshipDto.UserSummary.from(invite.getFromUser()),
                    GroupRelationshipDto.UserSummary.from(invite.getToUser()),
                    invite.getStatus().name(),
                    invite.getCreatedAt()
            );
        }
    }

    @Schema(description = "그룹 매핑 응답")
    public record GroupMappingResponse(
            @Schema(description = "관계 ID", example = "1")
            Long relationshipId,
            @Schema(description = "소속 그룹 목록")
            List<GroupRelationshipDto.GroupTag> groups
    ) {
    }

    @Schema(description = "그룹 전체 현황 응답")
    public record GroupOverviewResponse(
            @Schema(description = "그룹 요약 목록")
            List<GroupSummary> groups,
            @Schema(description = "그룹 상세 목록")
            List<GroupDetail> groupDetails,
            @Schema(description = "미분류 관계 목록")
            List<GroupRelationshipDto.RelationshipSummary> uncategorizedRelationships,
            @Schema(description = "받은 그룹 초대 목록")
            List<GroupInviteSummary> incomingGroupInvites,
            @Schema(description = "보낸 그룹 초대 목록")
            List<GroupInviteSummary> outgoingGroupInvites
    ) {
    }
}
