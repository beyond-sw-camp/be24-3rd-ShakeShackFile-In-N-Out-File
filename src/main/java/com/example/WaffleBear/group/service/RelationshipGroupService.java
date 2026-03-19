package com.example.WaffleBear.group.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.group.model.dto.GroupRelationshipDto;
import com.example.WaffleBear.group.model.dto.RelationshipGroupDto;
import com.example.WaffleBear.group.model.entity.Relationship;
import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import com.example.WaffleBear.group.model.entity.RelationshipGroupInvite;
import com.example.WaffleBear.group.model.entity.RelationshipGroupMapping;
import com.example.WaffleBear.group.model.enums.InviteStatus;
import com.example.WaffleBear.group.model.enums.RelationshipStatus;
import com.example.WaffleBear.group.repository.RelationshipGroupInviteRepository;
import com.example.WaffleBear.group.repository.RelationshipGroupMappingRepository;
import com.example.WaffleBear.group.repository.RelationshipGroupRepository;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_ACCESS_DENIED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_INVITE_ALREADY_PROCESSED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_INVITE_NOT_FOUND;
import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_NAME_DUPLICATED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_NOT_FOUND;
import static com.example.WaffleBear.common.model.BaseResponseStatus.RELATIONSHIP_REQUIRED;
import static com.example.WaffleBear.common.model.BaseResponseStatus.REQUEST_ERROR;
import static com.example.WaffleBear.common.model.BaseResponseStatus.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class RelationshipGroupService {

    private final RelationshipGroupRepository relationshipGroupRepository;
    private final RelationshipGroupMappingRepository relationshipGroupMappingRepository;
    private final RelationshipGroupInviteRepository relationshipGroupInviteRepository;
    private final GroupRelationshipService groupRelationshipService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public RelationshipGroupDto.GroupOverviewResponse getOverview(Long userId) {
        List<Relationship> activeRelationships = groupRelationshipService.getActiveRelationships(userId);
        List<RelationshipGroup> groups = relationshipGroupRepository.findAllByUser_IdxOrderByCreatedAtAsc(userId);
        List<RelationshipGroupMapping> mappings = relationshipGroupMappingRepository.findAllByRelationship_User_Idx(userId);

        Set<Long> activeRelationshipIds = activeRelationships.stream().map(Relationship::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, List<GroupRelationshipDto.GroupTag>> tagsByRelationshipId = new LinkedHashMap<>();
        Map<Long, Long> countsByGroupId = new LinkedHashMap<>();

        mappings.forEach(mapping -> {
            if (!activeRelationshipIds.contains(mapping.getRelationship().getId()) || mapping.getGroup() == null) {
                return;
            }

            tagsByRelationshipId
                    .computeIfAbsent(mapping.getRelationship().getId(), ignored -> new ArrayList<>())
                    .add(GroupRelationshipDto.GroupTag.from(mapping.getGroup()));
            countsByGroupId.merge(mapping.getGroup().getId(), 1L, Long::sum);
        });

        List<GroupRelationshipDto.RelationshipSummary> relationshipSummaries = activeRelationships.stream()
                .map(relationship -> GroupRelationshipDto.RelationshipSummary.from(
                        relationship,
                        tagsByRelationshipId.getOrDefault(relationship.getId(), List.of())
                ))
                .toList();

        Map<Long, List<GroupRelationshipDto.RelationshipSummary>> groupRelationships = new LinkedHashMap<>();
        relationshipSummaries.forEach(summary ->
                summary.groups().forEach(groupTag ->
                        groupRelationships.computeIfAbsent(groupTag.groupId(), ignored -> new ArrayList<>()).add(summary)
                )
        );

        List<RelationshipGroupDto.GroupSummary> groupSummaries = groups.stream()
                .map(group -> RelationshipGroupDto.GroupSummary.from(group, countsByGroupId.getOrDefault(group.getId(), 0L)))
                .toList();

        List<RelationshipGroupDto.GroupDetail> groupDetails = groups.stream()
                .map(group -> RelationshipGroupDto.GroupDetail.from(group, groupRelationships.getOrDefault(group.getId(), List.of())))
                .toList();

        List<GroupRelationshipDto.RelationshipSummary> uncategorizedRelationships = relationshipSummaries.stream()
                .filter(summary -> summary.groups() == null || summary.groups().isEmpty())
                .toList();

        return new RelationshipGroupDto.GroupOverviewResponse(
                groupSummaries,
                groupDetails,
                uncategorizedRelationships,
                relationshipGroupInviteRepository.findAllByToUser_IdxOrderByCreatedAtDesc(userId)
                        .stream()
                        .map(RelationshipGroupDto.GroupInviteSummary::from)
                        .toList(),
                relationshipGroupInviteRepository.findAllByFromUser_IdxOrderByCreatedAtDesc(userId)
                        .stream()
                        .map(RelationshipGroupDto.GroupInviteSummary::from)
                        .toList()
        );
    }

    @Transactional
    public RelationshipGroupDto.GroupSummary createGroup(Long userId, RelationshipGroupDto.CreateGroupRequest request) {
        User user = getUser(userId);
        String name = normalizeGroupName(request != null ? request.name() : null);

        if (relationshipGroupRepository.existsByUser_IdxAndNameIgnoreCase(userId, name)) {
            throw BaseException.from(GROUP_NAME_DUPLICATED);
        }

        RelationshipGroup group = relationshipGroupRepository.save(RelationshipGroup.builder()
                .user(user)
                .name(name)
                .build());

        return RelationshipGroupDto.GroupSummary.from(group, 0);
    }

    @Transactional
    public RelationshipGroupDto.GroupSummary renameGroup(Long userId, Long groupId, RelationshipGroupDto.UpdateGroupRequest request) {
        RelationshipGroup group = getOwnedGroup(userId, groupId);
        String nextName = normalizeGroupName(request != null ? request.name() : null);

        if (!group.getName().equalsIgnoreCase(nextName)
                && relationshipGroupRepository.existsByUser_IdxAndNameIgnoreCase(userId, nextName)) {
            throw BaseException.from(GROUP_NAME_DUPLICATED);
        }

        group.rename(nextName);
        relationshipGroupRepository.save(group);

        long relationshipCount = relationshipGroupMappingRepository.findAllByGroup_Id(groupId).size();
        return RelationshipGroupDto.GroupSummary.from(group, relationshipCount);
    }

    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        RelationshipGroup group = getOwnedGroup(userId, groupId);
        relationshipGroupMappingRepository.deleteAllByGroup_Id(groupId);
        relationshipGroupRepository.delete(group);
    }

    @Transactional
    public RelationshipGroupDto.GroupMappingResponse addRelationshipToGroup(
            Long userId,
            Long relationshipId,
            RelationshipGroupDto.AddGroupMappingRequest request
    ) {
        Relationship relationship = groupRelationshipService.getOwnedRelationship(userId, relationshipId);
        RelationshipGroup group = getOwnedGroup(userId, request != null ? request.groupId() : null);

        if (!relationshipGroupMappingRepository.existsByRelationship_IdAndGroup_Id(relationshipId, group.getId())) {
            relationshipGroupMappingRepository.save(RelationshipGroupMapping.builder()
                    .relationship(relationship)
                    .group(group)
                    .build());
        }

        return buildMappingResponse(relationshipId);
    }

    @Transactional
    public RelationshipGroupDto.GroupMappingResponse replaceRelationshipGroups(
            Long userId,
            Long relationshipId,
            RelationshipGroupDto.ReplaceGroupMappingsRequest request
    ) {
        Relationship relationship = groupRelationshipService.getOwnedRelationship(userId, relationshipId);
        List<Long> groupIds = request != null && request.groupIds() != null
                ? request.groupIds().stream().filter(Objects::nonNull).distinct().toList()
                : List.of();

        List<RelationshipGroup> groups = groupIds.stream()
                .map(groupId -> getOwnedGroup(userId, groupId))
                .toList();

        relationshipGroupMappingRepository.deleteAllByRelationship_Id(relationship.getId());
        groups.forEach(group -> relationshipGroupMappingRepository.save(RelationshipGroupMapping.builder()
                .relationship(relationship)
                .group(group)
                .build()));

        return buildMappingResponse(relationshipId);
    }

    @Transactional
    public RelationshipGroupDto.GroupMappingResponse removeRelationshipFromGroup(Long userId, Long relationshipId, Long groupId) {
        groupRelationshipService.getOwnedRelationship(userId, relationshipId);
        getOwnedGroup(userId, groupId);
        relationshipGroupMappingRepository.deleteByRelationship_IdAndGroup_Id(relationshipId, groupId);
        return buildMappingResponse(relationshipId);
    }

    @Transactional
    public RelationshipGroupDto.GroupInviteSummary createGroupInvite(
            Long userId,
            RelationshipGroupDto.CreateGroupInviteRequest request
    ) {
        RelationshipGroup group = getOwnedGroup(userId, request != null ? request.groupId() : null);
        User targetUser = getUser(request != null ? request.toUserId() : null);

        if (Objects.equals(group.getUser().getIdx(), targetUser.getIdx())) {
            throw BaseException.from(REQUEST_ERROR);
        }

        Relationship relationship = groupRelationshipService.requireActiveRelationships(userId, List.of(targetUser.getIdx()))
                .stream()
                .findFirst()
                .orElseThrow(() -> BaseException.from(RELATIONSHIP_REQUIRED));

        if (relationship.getStatus() != RelationshipStatus.ACTIVE) {
            throw BaseException.from(RELATIONSHIP_REQUIRED);
        }

        RelationshipGroupInvite existingInvite = relationshipGroupInviteRepository
                .findByGroup_IdAndToUser_IdxAndStatus(group.getId(), targetUser.getIdx(), InviteStatus.PENDING)
                .orElse(null);
        if (existingInvite != null) {
            return RelationshipGroupDto.GroupInviteSummary.from(existingInvite);
        }

        RelationshipGroupInvite invite = relationshipGroupInviteRepository.save(RelationshipGroupInvite.builder()
                .group(group)
                .fromUser(group.getUser())
                .toUser(targetUser)
                .status(InviteStatus.PENDING)
                .build());

        notificationService.sendGroupInviteNotification(
                targetUser.getIdx(),
                invite.getId(),
                group.getName(),
                group.getUser().getName()
        );

        return RelationshipGroupDto.GroupInviteSummary.from(invite);
    }

    @Transactional
    public RelationshipGroupDto.GroupInviteSummary acceptGroupInvite(Long userId, Long inviteId) {
        RelationshipGroupInvite invite = getGroupInviteForRecipient(userId, inviteId);

        if (invite.getStatus() == InviteStatus.REJECTED) {
            throw BaseException.from(GROUP_INVITE_ALREADY_PROCESSED);
        }

        if (invite.getStatus() == InviteStatus.PENDING) {
            Relationship relationship = groupRelationshipService.requireActiveRelationships(
                    invite.getGroup().getUser().getIdx(),
                    List.of(userId)
            ).stream().findFirst().orElseThrow(() -> BaseException.from(RELATIONSHIP_REQUIRED));

            if (!relationshipGroupMappingRepository.existsByRelationship_IdAndGroup_Id(
                    relationship.getId(),
                    invite.getGroup().getId()
            )) {
                relationshipGroupMappingRepository.save(RelationshipGroupMapping.builder()
                        .relationship(relationship)
                        .group(invite.getGroup())
                        .build());
            }

            invite.accept();
            relationshipGroupInviteRepository.save(invite);

            notificationService.sendGeneralNotification(
                    invite.getFromUser().getIdx(),
                    "그룹 초대 수락",
                    invite.getToUser().getName() + "님이 [" + invite.getGroup().getName() + "] 그룹 초대를 수락했습니다."
            );
        }

        return RelationshipGroupDto.GroupInviteSummary.from(invite);
    }

    @Transactional
    public RelationshipGroupDto.GroupInviteSummary rejectGroupInvite(Long userId, Long inviteId) {
        RelationshipGroupInvite invite = getGroupInviteForRecipient(userId, inviteId);

        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw BaseException.from(GROUP_INVITE_ALREADY_PROCESSED);
        }

        if (invite.getStatus() == InviteStatus.PENDING) {
            invite.reject();
            relationshipGroupInviteRepository.save(invite);

            notificationService.sendGeneralNotification(
                    invite.getFromUser().getIdx(),
                    "그룹 초대 거절",
                    invite.getToUser().getName() + "님이 [" + invite.getGroup().getName() + "] 그룹 초대를 거절했습니다."
            );
        }

        return RelationshipGroupDto.GroupInviteSummary.from(invite);
    }

    private RelationshipGroupDto.GroupMappingResponse buildMappingResponse(Long relationshipId) {
        List<GroupRelationshipDto.GroupTag> groups = relationshipGroupMappingRepository.findAllByRelationship_Id(relationshipId)
                .stream()
                .filter(mapping -> mapping.getGroup() != null)
                .map(mapping -> GroupRelationshipDto.GroupTag.from(mapping.getGroup()))
                .toList();

        return new RelationshipGroupDto.GroupMappingResponse(relationshipId, groups);
    }

    private RelationshipGroupInvite getGroupInviteForRecipient(Long userId, Long inviteId) {
        RelationshipGroupInvite invite = relationshipGroupInviteRepository.findById(inviteId)
                .orElseThrow(() -> BaseException.from(GROUP_INVITE_NOT_FOUND));

        if (!Objects.equals(invite.getToUser().getIdx(), userId)) {
            throw BaseException.from(GROUP_ACCESS_DENIED);
        }

        return invite;
    }

    private RelationshipGroup getOwnedGroup(Long userId, Long groupId) {
        if (groupId == null) {
            throw BaseException.from(GROUP_NOT_FOUND);
        }

        return relationshipGroupRepository.findByIdAndUser_Idx(groupId, userId)
                .orElseThrow(() -> BaseException.from(GROUP_NOT_FOUND));
    }

    private String normalizeGroupName(String name) {
        if (name == null || name.isBlank()) {
            throw BaseException.from(REQUEST_ERROR);
        }

        return name.trim();
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw BaseException.from(USER_NOT_FOUND);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> BaseException.from(USER_NOT_FOUND));
    }
}
