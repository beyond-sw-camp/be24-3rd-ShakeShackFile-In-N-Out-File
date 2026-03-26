package com.example.WaffleBear.group.model.dto;

import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import com.example.WaffleBear.group.model.entity.RelationshipGroupInvite;

import java.time.LocalDateTime;
import java.util.List;

public class RelationshipGroupDto {

    public record CreateGroupRequest(
            String name
    ) {
    }

    public record UpdateGroupRequest(
            String name
    ) {
    }

    public record AddGroupMappingRequest(
            Long groupId
    ) {
    }

    public record ReplaceGroupMappingsRequest(
            List<Long> groupIds
    ) {
    }

    public record CreateGroupInviteRequest(
            Long groupId,
            Long toUserId
    ) {
    }

    public record GroupSummary(
            Long groupId,
            String name,
            LocalDateTime createdAt,
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

    public record GroupDetail(
            Long groupId,
            String name,
            LocalDateTime createdAt,
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

    public record GroupInviteSummary(
            Long groupInviteId,
            Long groupId,
            String groupName,
            GroupRelationshipDto.UserSummary fromUser,
            GroupRelationshipDto.UserSummary toUser,
            String status,
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

    public record GroupMappingResponse(
            Long relationshipId,
            List<GroupRelationshipDto.GroupTag> groups
    ) {
    }

    public record GroupOverviewResponse(
            List<GroupSummary> groups,
            List<GroupDetail> groupDetails,
            List<GroupRelationshipDto.RelationshipSummary> uncategorizedRelationships,
            List<GroupInviteSummary> incomingGroupInvites,
            List<GroupInviteSummary> outgoingGroupInvites
    ) {
    }
}
