package com.example.WaffleBear.group.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.group.model.dto.GroupRelationshipDto;
import com.example.WaffleBear.group.model.entity.Relationship;
import com.example.WaffleBear.group.model.entity.RelationshipInvite;
import com.example.WaffleBear.group.model.enums.InviteStatus;
import com.example.WaffleBear.group.model.enums.InviteType;
import com.example.WaffleBear.group.model.enums.RelationshipStatus;
import com.example.WaffleBear.group.repository.RelationshipGroupMappingRepository;
import com.example.WaffleBear.group.repository.RelationshipInviteRepository;
import com.example.WaffleBear.group.repository.RelationshipRepository;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.example.WaffleBear.common.model.BaseResponseStatus.DUPLICATE_RELATIONSHIP;
import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_ACCESS_DENIED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_RELATIONSHIP_NOT_FOUND;
import static com.example.WaffleBear.common.model.BaseResponseStatus.RELATIONSHIP_INVITE_ALREADY_PROCESSED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.RELATIONSHIP_INVITE_NOT_FOUND;
import static com.example.WaffleBear.common.model.BaseResponseStatus.RELATIONSHIP_REQUIRED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.REQUEST_ERROR;
import static com.example.WaffleBear.common.model.BaseResponseStatus.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GroupRelationshipService {

    private final RelationshipInviteRepository relationshipInviteRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipGroupMappingRepository relationshipGroupMappingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public GroupRelationshipDto.RelationshipListResponse getRelationshipList(Long userId) {
        User user = getUser(userId);

        List<Relationship> relationships = relationshipRepository.findAllByUser_IdxAndStatusOrderByCreatedAtDesc(
                userId,
                RelationshipStatus.ACTIVE
        );
        List<RelationshipInvite> incomingInvites = collectIncomingInvites(user);
        List<RelationshipInvite> outgoingInvites = relationshipInviteRepository.findAllByFromUser_IdxOrderByCreatedAtDesc(userId);

        return new GroupRelationshipDto.RelationshipListResponse(
                toRelationshipSummaries(relationships),
                incomingInvites.stream().map(GroupRelationshipDto.InviteSummary::from).toList(),
                outgoingInvites.stream().map(GroupRelationshipDto.InviteSummary::from).toList()
        );
    }

    @Transactional
    public GroupRelationshipDto.InviteSummary createInvite(Long userId, GroupRelationshipDto.CreateInviteRequest request) {
        User actor = getUser(userId);
        InviteType inviteType = request.type() != null ? request.type() : InviteType.FRIEND;
        String normalizedEmail = normalizeEmail(request.email());

        User targetUser = null;
        if (request.toUserId() != null) {
            targetUser = getUser(request.toUserId());
            normalizedEmail = targetUser.getEmail();
        } else if (normalizedEmail != null) {
            targetUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        }

        if (targetUser == null && normalizedEmail == null) {
            throw BaseException.from(REQUEST_ERROR);
        }

        if (targetUser != null && Objects.equals(actor.getIdx(), targetUser.getIdx())) {
            throw BaseException.from(REQUEST_ERROR);
        }

        if (targetUser != null) {
            Relationship existing = relationshipRepository.findByUser_IdxAndTargetUser_Idx(userId, targetUser.getIdx())
                    .orElse(null);
            if (existing != null && existing.getStatus() == RelationshipStatus.ACTIVE) {
                throw BaseException.from(DUPLICATE_RELATIONSHIP);
            }
        }

        RelationshipInvite pending = findPendingInvite(userId, targetUser != null ? targetUser.getIdx() : null, normalizedEmail, inviteType);
        if (pending != null) {
            return GroupRelationshipDto.InviteSummary.from(pending);
        }

        RelationshipInvite invite = relationshipInviteRepository.save(RelationshipInvite.builder()
                .fromUser(actor)
                .toUser(targetUser)
                .email(normalizedEmail)
                .type(inviteType)
                .status(InviteStatus.PENDING)
                .build());

        if (targetUser != null) {
            notificationService.sendRelationshipInviteNotification(targetUser.getIdx(), invite.getId(), actor.getName());
        }

        return GroupRelationshipDto.InviteSummary.from(invite);
    }

    @Transactional
    public GroupRelationshipDto.InviteActionResponse acceptInvite(Long userId, Long inviteId) {
        User actor = getUser(userId);
        RelationshipInvite invite = getAccessibleInvite(actor, inviteId);

        if (invite.getStatus() == InviteStatus.REJECTED) {
            throw BaseException.from(RELATIONSHIP_INVITE_ALREADY_PROCESSED);
        }

        if (invite.getStatus() == InviteStatus.PENDING) {
            invite.bindToUser(actor);
            invite.accept();
            relationshipInviteRepository.save(invite);

            createOrUpdateRelationshipPair(invite.getFromUser(), actor, RelationshipStatus.ACTIVE);
            notificationService.sendGeneralNotification(
                    invite.getFromUser().getIdx(),
                    "연결 요청 수락",
                    actor.getName() + "님이 연결 요청을 수락했습니다."
            );
        }

        return new GroupRelationshipDto.InviteActionResponse(
                GroupRelationshipDto.InviteSummary.from(invite),
                toRelationshipSummaries(relationshipRepository.findAllByUser_IdxOrderByCreatedAtDesc(actor.getIdx()))
        );
    }

    @Transactional
    public GroupRelationshipDto.InviteSummary rejectInvite(Long userId, Long inviteId) {
        User actor = getUser(userId);
        RelationshipInvite invite = getAccessibleInvite(actor, inviteId);

        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw BaseException.from(RELATIONSHIP_INVITE_ALREADY_PROCESSED);
        }

        if (invite.getStatus() == InviteStatus.PENDING) {
            invite.bindToUser(actor);
            invite.reject();
            relationshipInviteRepository.save(invite);

            notificationService.sendGeneralNotification(
                    invite.getFromUser().getIdx(),
                    "연결 요청 거절",
                    actor.getName() + "님이 연결 요청을 거절했습니다."
            );
        }

        return GroupRelationshipDto.InviteSummary.from(invite);
    }

    @Transactional
    public GroupRelationshipDto.RelationshipSummary updateRelationshipStatus(
            Long userId,
            Long relationshipId,
            GroupRelationshipDto.UpdateRelationshipStatusRequest request
    ) {
        Relationship relationship = getOwnedRelationship(userId, relationshipId);
        RelationshipStatus nextStatus = request != null && request.status() != null
                ? request.status()
                : RelationshipStatus.ACTIVE;

        relationship.updateStatus(nextStatus);
        relationshipRepository.save(relationship);

        return toRelationshipSummary(relationship);
    }

    @Transactional
    public void deleteRelationship(Long userId, Long relationshipId) {
        Relationship relationship = getOwnedRelationship(userId, relationshipId);
        Long targetUserId = relationship.getTargetUser().getIdx();

        relationshipGroupMappingRepository.deleteAllByRelationship_Id(relationship.getId());
        relationship.updateStatus(RelationshipStatus.BLOCKED);
        relationshipRepository.save(relationship);

        Relationship reverseRelationship = relationshipRepository.findByUser_IdxAndTargetUser_Idx(targetUserId, userId)
                .orElse(null);

        if (reverseRelationship != null) {
            relationshipGroupMappingRepository.deleteAllByRelationship_Id(reverseRelationship.getId());
            reverseRelationship.updateStatus(RelationshipStatus.BLOCKED);
            relationshipRepository.save(reverseRelationship);
        }
    }

    @Transactional
    public Relationship createActiveRelationshipPair(Long userId, Long targetUserId) {
        User actor = getUser(userId);
        User target = getUser(targetUserId);

        if (Objects.equals(actor.getIdx(), target.getIdx())) {
            throw BaseException.from(REQUEST_ERROR);
        }

        return createOrUpdateRelationshipPair(actor, target, RelationshipStatus.ACTIVE);
    }

    @Transactional
    public RelationshipInvite createPendingEmailInvite(Long userId, String email, InviteType inviteType) {
        User actor = getUser(userId);
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw BaseException.from(REQUEST_ERROR);
        }

        RelationshipInvite existing = findPendingInvite(userId, null, normalizedEmail, inviteType);
        if (existing != null) {
            return existing;
        }

        return relationshipInviteRepository.save(RelationshipInvite.builder()
                .fromUser(actor)
                .email(normalizedEmail)
                .type(inviteType)
                .status(InviteStatus.PENDING)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Relationship> requireActiveRelationships(Long userId, Collection<Long> targetUserIds) {
        List<Long> normalizedTargetIds = targetUserIds == null
                ? List.of()
                : targetUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalizedTargetIds.isEmpty()) {
            return List.of();
        }

        List<Relationship> relationships =
                relationshipRepository.findAllByUser_IdxAndTargetUser_IdxIn(userId, normalizedTargetIds);

        Map<Long, Relationship> relationshipByTargetId = new LinkedHashMap<>();
        relationships.forEach(relationship -> relationshipByTargetId.put(relationship.getTargetUser().getIdx(), relationship));

        for (Long targetUserId : normalizedTargetIds) {
            Relationship relationship = relationshipByTargetId.get(targetUserId);
            if (relationship == null || relationship.getStatus() != RelationshipStatus.ACTIVE) {
                throw BaseException.from(RELATIONSHIP_REQUIRED);
            }
        }

        return normalizedTargetIds.stream()
                .map(relationshipByTargetId::get)
                .toList();
    }

    @Transactional(readOnly = true)
    public Relationship getOwnedRelationship(Long userId, Long relationshipId) {
        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> BaseException.from(GROUP_RELATIONSHIP_NOT_FOUND));

        if (!Objects.equals(relationship.getUser().getIdx(), userId)) {
            throw BaseException.from(GROUP_ACCESS_DENIED);
        }

        return relationship;
    }

    @Transactional(readOnly = true)
    public List<Relationship> getActiveRelationships(Long userId) {
        return relationshipRepository.findAllByUser_IdxAndStatusOrderByCreatedAtDesc(userId, RelationshipStatus.ACTIVE);
    }

    private Relationship createOrUpdateRelationshipPair(User left, User right, RelationshipStatus status) {
        Relationship direct = upsertRelationship(left, right, status);
        upsertRelationship(right, left, status);
        return direct;
    }

    private Relationship upsertRelationship(User user, User target, RelationshipStatus status) {
        Relationship relationship = relationshipRepository.findByUser_IdxAndTargetUser_Idx(user.getIdx(), target.getIdx())
                .orElse(Relationship.builder()
                        .user(user)
                        .targetUser(target)
                        .status(status)
                        .build());

        relationship.updateStatus(status);
        return relationshipRepository.save(relationship);
    }

    private RelationshipInvite getAccessibleInvite(User user, Long inviteId) {
        RelationshipInvite invite = relationshipInviteRepository.findById(inviteId)
                .orElseThrow(() -> BaseException.from(RELATIONSHIP_INVITE_NOT_FOUND));

        boolean matchesUser = invite.getToUser() != null && Objects.equals(invite.getToUser().getIdx(), user.getIdx());
        boolean matchesEmail = invite.getEmail() != null
                && user.getEmail() != null
                && invite.getEmail().equalsIgnoreCase(user.getEmail());

        if (!matchesUser && !matchesEmail) {
            throw BaseException.from(GROUP_ACCESS_DENIED);
        }

        return invite;
    }

    private RelationshipInvite findPendingInvite(Long userId, Long targetUserId, String email, InviteType inviteType) {
        if (targetUserId != null) {
            return relationshipInviteRepository.findByFromUser_IdxAndToUser_IdxAndTypeAndStatus(
                    userId,
                    targetUserId,
                    inviteType,
                    InviteStatus.PENDING
            ).orElse(null);
        }

        if (email != null) {
            return relationshipInviteRepository.findByFromUser_IdxAndEmailAndTypeAndStatus(
                    userId,
                    email,
                    inviteType,
                    InviteStatus.PENDING
            ).orElse(null);
        }

        return null;
    }

    private List<RelationshipInvite> collectIncomingInvites(User user) {
        Map<Long, RelationshipInvite> inviteMap = new LinkedHashMap<>();

        relationshipInviteRepository.findAllByToUser_IdxOrderByCreatedAtDesc(user.getIdx())
                .forEach(invite -> inviteMap.put(invite.getId(), invite));

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            relationshipInviteRepository.findAllByEmailOrderByCreatedAtDesc(user.getEmail())
                    .forEach(invite -> inviteMap.putIfAbsent(invite.getId(), invite));
        }

        return new ArrayList<>(inviteMap.values());
    }

    private List<GroupRelationshipDto.RelationshipSummary> toRelationshipSummaries(List<Relationship> relationships) {
        List<Long> relationshipIds = relationships.stream().map(Relationship::getId).toList();
        Map<Long, List<GroupRelationshipDto.GroupTag>> groupTagsByRelationshipId = new LinkedHashMap<>();

        relationshipGroupMappingRepository.findAllByRelationship_IdIn(relationshipIds).forEach(mapping -> {
            if (mapping.getGroup() == null) {
                return;
            }

            groupTagsByRelationshipId
                    .computeIfAbsent(mapping.getRelationship().getId(), ignored -> new ArrayList<>())
                    .add(GroupRelationshipDto.GroupTag.from(mapping.getGroup()));
        });

        return relationships.stream()
                .map(relationship -> GroupRelationshipDto.RelationshipSummary.from(
                        relationship,
                        groupTagsByRelationshipId.getOrDefault(relationship.getId(), List.of())
                ))
                .toList();
    }

    private GroupRelationshipDto.RelationshipSummary toRelationshipSummary(Relationship relationship) {
        return toRelationshipSummaries(List.of(relationship)).get(0);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BaseException.from(USER_NOT_FOUND));
    }
}
