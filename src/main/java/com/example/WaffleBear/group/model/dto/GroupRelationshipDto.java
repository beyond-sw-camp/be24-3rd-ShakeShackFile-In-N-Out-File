package com.example.WaffleBear.group.model.dto;

import com.example.WaffleBear.group.model.entity.Relationship;
import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import com.example.WaffleBear.group.model.entity.RelationshipInvite;
import com.example.WaffleBear.group.model.enums.InviteType;
import com.example.WaffleBear.group.model.enums.RelationshipStatus;
import com.example.WaffleBear.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class GroupRelationshipDto {

    @Schema(description = "사용자 요약 정보")
    public record UserSummary(
            @Schema(description = "사용자 IDX", example = "1")
            Long userId,
            @Schema(description = "사용자 이름", example = "홍길동")
            String name,
            @Schema(description = "사용자 이메일", example = "user@example.com")
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

    @Schema(description = "그룹 태그 정보")
    public record GroupTag(
            @Schema(description = "그룹 ID", example = "1")
            Long groupId,
            @Schema(description = "그룹명", example = "개발팀")
            String groupName
    ) {
        public static GroupTag from(RelationshipGroup group) {
            return new GroupTag(
                    group.getId(),
                    group.getName()
            );
        }
    }

    @Schema(description = "관계 초대 요청")
    public record CreateInviteRequest(
            @Schema(description = "초대할 사용자 IDX (직접 지정 시)", example = "5")
            Long toUserId,
            @Schema(description = "초대할 사용자 이메일 (이메일 지정 시)", example = "friend@example.com")
            String email,
            @Schema(description = "초대 유형", example = "FRIEND")
            InviteType type
    ) {
    }

    @Schema(description = "관계 상태 변경 요청")
    public record UpdateRelationshipStatusRequest(
            @Schema(description = "변경할 상태", example = "BLOCKED")
            RelationshipStatus status
    ) {
    }

    @Schema(description = "초대 요약 정보")
    public record InviteSummary(
            @Schema(description = "초대 ID", example = "1")
            Long inviteId,
            @Schema(description = "초대 보낸 사용자")
            UserSummary fromUser,
            @Schema(description = "초대 받은 사용자")
            UserSummary toUser,
            @Schema(description = "초대 이메일", example = "friend@example.com")
            String email,
            @Schema(description = "초대 유형", example = "FRIEND")
            String type,
            @Schema(description = "초대 상태", example = "PENDING")
            String status,
            @Schema(description = "초대 생성 일시")
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

    @Schema(description = "관계 요약 정보")
    public record RelationshipSummary(
            @Schema(description = "관계 ID", example = "1")
            Long relationshipId,
            @Schema(description = "대상 사용자 정보")
            UserSummary targetUser,
            @Schema(description = "관계 상태", example = "ACTIVE")
            String status,
            @Schema(description = "관계 생성 일시")
            LocalDateTime createdAt,
            @Schema(description = "소속 그룹 목록")
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

    @Schema(description = "초대 수락/거절 응답")
    public record InviteActionResponse(
            @Schema(description = "처리된 초대 정보")
            InviteSummary invite,
            @Schema(description = "갱신된 관계 목록")
            List<RelationshipSummary> relationships
    ) {
    }

    @Schema(description = "관계 목록 응답")
    public record RelationshipListResponse(
            @Schema(description = "관계 목록")
            List<RelationshipSummary> relationships,
            @Schema(description = "받은 초대 목록")
            List<InviteSummary> incomingInvites,
            @Schema(description = "보낸 초대 목록")
            List<InviteSummary> outgoingInvites
    ) {
    }
}
