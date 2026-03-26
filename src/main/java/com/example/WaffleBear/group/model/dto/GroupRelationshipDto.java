package com.example.WaffleBear.group.model.dto;

import com.example.WaffleBear.group.model.entity.Relationship;
import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import com.example.WaffleBear.group.model.entity.RelationshipInvite;
import com.example.WaffleBear.group.model.enums.InviteType;
import com.example.WaffleBear.group.model.enums.RelationshipStatus;
import com.example.WaffleBear.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

public class GroupRelationshipDto {

    public record UserSummary(
            Long userId,
            String name,
            String email
    ) {
        public static UserSummary from(User user) {
            if (user == null) {
                return null;
            }

            return new UserSummary(
                    user.getIdx(),
                    user.getName(),
                    user.getEmail()
            );
        }
    }

    public record GroupTag(
            Long groupId,
            String groupName
    ) {
        public static GroupTag from(RelationshipGroup group) {
            return new GroupTag(
                    group.getId(),
                    group.getName()
            );
        }
    }

    public record CreateInviteRequest(
            Long toUserId,
            String email,
            InviteType type
    ) {
    }

    public record UpdateRelationshipStatusRequest(
            RelationshipStatus status
    ) {
    }

    public record InviteSummary(
            Long inviteId,
            UserSummary fromUser,
            UserSummary toUser,
            String email,
            String type,
            String status,
            LocalDateTime createdAt
    ) {
        public static InviteSummary from(RelationshipInvite invite) {
            return new InviteSummary(
                    invite.getId(),
                    UserSummary.from(invite.getFromUser()),
                    UserSummary.from(invite.getToUser()),
                    invite.getEmail(),
                    invite.getType().name(),
                    invite.getStatus().name(),
                    invite.getCreatedAt()
            );
        }
    }

    public record RelationshipSummary(
            Long relationshipId,
            UserSummary targetUser,
            String status,
            LocalDateTime createdAt,
            List<GroupTag> groups
    ) {
        public static RelationshipSummary from(Relationship relationship, List<GroupTag> groups) {
            return new RelationshipSummary(
                    relationship.getId(),
                    UserSummary.from(relationship.getTargetUser()),
                    relationship.getStatus().name(),
                    relationship.getCreatedAt(),
                    groups
            );
        }
    }

    public record InviteActionResponse(
            InviteSummary invite,
            List<RelationshipSummary> relationships
    ) {
    }

    public record RelationshipListResponse(
            List<RelationshipSummary> relationships,
            List<InviteSummary> incomingInvites,
            List<InviteSummary> outgoingInvites
    ) {
    }
}
