package com.example.WaffleBear.group.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.file.share.ShareService;
import com.example.WaffleBear.group.model.dto.GroupRelationshipDto;
import com.example.WaffleBear.group.model.dto.GroupShareDto;
import com.example.WaffleBear.group.model.dto.RelationshipGroupDto;
import com.example.WaffleBear.group.model.entity.Relationship;
import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import com.example.WaffleBear.group.model.entity.RelationshipGroupInvite;
import com.example.WaffleBear.group.model.entity.RelationshipGroupMapping;
import com.example.WaffleBear.group.model.entity.RelationshipInvite;
import com.example.WaffleBear.group.model.enums.InviteStatus;
import com.example.WaffleBear.group.model.enums.InviteType;
import com.example.WaffleBear.group.model.enums.RelationshipStatus;
import com.example.WaffleBear.group.repository.RelationshipGroupInviteRepository;
import com.example.WaffleBear.group.repository.RelationshipGroupMappingRepository;
import com.example.WaffleBear.group.repository.RelationshipGroupRepository;
import com.example.WaffleBear.group.repository.RelationshipRepository;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.workspace.service.PostService;
import com.example.WaffleBear.chat.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.example.WaffleBear.common.model.BaseResponseStatus.GROUP_NOT_FOUND;
import static com.example.WaffleBear.common.model.BaseResponseStatus.REQUEST_ERROR;
import static com.example.WaffleBear.common.model.BaseResponseStatus.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GroupShareService {

    private final GroupRelationshipService groupRelationshipService;
    private final RelationshipGroupRepository relationshipGroupRepository;
    private final RelationshipGroupInviteRepository relationshipGroupInviteRepository;
    private final RelationshipGroupMappingRepository relationshipGroupMappingRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final ShareService shareService;
    private final PostService postService;
    private final ChatRoomService chatRoomService;

    @Transactional
    public GroupShareDto.ShareResult shareFiles(Long userId, GroupShareDto.FileShareRequest request) {
        if (request == null || request.fileIds() == null || request.fileIds().isEmpty()) {
            throw BaseException.from(REQUEST_ERROR);
        }

        RecipientResolution resolution = resolveRecipients(
                userId,
                request.userIds(),
                request.groupIds(),
                request.emails(),
                InviteType.FILE
        );

        int affectedCount = shareService.shareFilesToUsers(userId, request.fileIds(), resolution.userIds());
        return new GroupShareDto.ShareResult(affectedCount, resolution.recipients(), resolution.pendingInvites());
    }

    @Transactional
    public GroupShareDto.ShareResult shareWorkspace(Long userId, GroupShareDto.WorkspaceShareRequest request) {
        if (request == null || request.workspaceId() == null) {
            throw BaseException.from(REQUEST_ERROR);
        }

        RecipientResolution resolution = resolveRecipients(
                userId,
                request.userIds(),
                request.groupIds(),
                request.emails(),
                InviteType.WORKSPACE
        );

        int affectedCount = postService.shareWithUsers(request.workspaceId(), userId, resolution.userIds());
        return new GroupShareDto.ShareResult(affectedCount, resolution.recipients(), resolution.pendingInvites());
    }

    @Transactional
    public GroupShareDto.ShareResult shareChat(Long userId, GroupShareDto.ChatShareRequest request) {
        if (request == null || request.roomId() == null) {
            throw BaseException.from(REQUEST_ERROR);
        }

        RecipientResolution resolution = resolveChatRecipients(userId, request);

        int affectedCount = chatRoomService.inviteUsers(request.roomId(), userId, resolution.userIds());
        return new GroupShareDto.ShareResult(affectedCount, resolution.recipients(), resolution.pendingInvites());
    }

    @Transactional(readOnly = true)
    public RelationshipGroupDto.GroupOverviewResponse getChatShareOverview(Long userId) {
        List<Relationship> directRelationships = groupRelationshipService.getActiveRelationships(userId);
        Map<Long, Relationship> directRelationshipByUserId = new LinkedHashMap<>();
        directRelationships.forEach(relationship -> directRelationshipByUserId.put(
                relationship.getTargetUser().getIdx(),
                relationship
        ));

        Map<Long, RelationshipGroup> accessibleGroups = resolveAccessibleChatGroups(userId);
        Map<Long, List<GroupRelationshipDto.RelationshipSummary>> groupDetailsById = new LinkedHashMap<>();
        Set<Long> groupedUserIds = new LinkedHashSet<>();

        accessibleGroups.values().forEach(group -> {
            List<GroupRelationshipDto.RelationshipSummary> members = buildChatGroupMembers(
                    userId,
                    group,
                    directRelationshipByUserId
            );
            groupDetailsById.put(group.getId(), members);
            members.stream()
                    .map(GroupRelationshipDto.RelationshipSummary::targetUser)
                    .filter(Objects::nonNull)
                    .map(GroupRelationshipDto.UserSummary::userId)
                    .filter(Objects::nonNull)
                    .forEach(groupedUserIds::add);
        });

        List<RelationshipGroupDto.GroupSummary> groups = accessibleGroups.values().stream()
                .map(group -> RelationshipGroupDto.GroupSummary.from(
                        group,
                        groupDetailsById.getOrDefault(group.getId(), List.of()).size()
                ))
                .toList();

        List<RelationshipGroupDto.GroupDetail> groupDetails = accessibleGroups.values().stream()
                .map(group -> RelationshipGroupDto.GroupDetail.from(
                        group,
                        groupDetailsById.getOrDefault(group.getId(), List.of())
                ))
                .toList();

        List<GroupRelationshipDto.RelationshipSummary> uncategorizedRelationships = directRelationships.stream()
                .filter(relationship -> !groupedUserIds.contains(relationship.getTargetUser().getIdx()))
                .map(relationship -> GroupRelationshipDto.RelationshipSummary.from(relationship, List.of()))
                .toList();

        return new RelationshipGroupDto.GroupOverviewResponse(
                groups,
                groupDetails,
                uncategorizedRelationships,
                List.of(),
                List.of()
        );
    }

    private RecipientResolution resolveRecipients(
            Long userId,
            List<Long> userIds,
            List<Long> groupIds,
            List<String> emails,
            InviteType inviteType
    ) {
        User actor = getUser(userId);
        Map<Long, GroupShareDto.ShareRecipient> recipientByUserId = new LinkedHashMap<>();
        List<GroupShareDto.PendingInvite> pendingInvites = new ArrayList<>();

        groupRelationshipService.requireActiveRelationships(userId, userIds)
                .forEach(relationship -> addRecipient(
                        recipientByUserId,
                        relationship.getTargetUser(),
                        "user"
                ));

        normalizeGroupIds(groupIds).forEach(groupId -> {
            RelationshipGroup group = relationshipGroupRepository.findByIdAndUser_Idx(groupId, userId)
                    .orElseThrow(() -> BaseException.from(GROUP_NOT_FOUND));

            List<RelationshipGroupMapping> mappings = relationshipGroupMappingRepository.findAllByGroup_Id(group.getId());
            mappings.stream()
                    .map(RelationshipGroupMapping::getRelationship)
                    .filter(relationship -> Objects.equals(relationship.getUser().getIdx(), userId))
                    .filter(relationship -> relationship.getStatus() == RelationshipStatus.ACTIVE)
                    .forEach(relationship -> addRecipient(
                            recipientByUserId,
                            relationship.getTargetUser(),
                            "group:" + group.getName()
                    ));
        });

        normalizeEmails(emails).forEach(email -> {
            if (actor.getEmail() != null && actor.getEmail().equalsIgnoreCase(email)) {
                return;
            }

            User targetUser = userRepository.findByEmail(email).orElse(null);
            if (targetUser == null) {
                RelationshipInvite invite = groupRelationshipService.createPendingEmailInvite(userId, email, inviteType);
                pendingInvites.add(new GroupShareDto.PendingInvite(invite.getId(), email, inviteType));
                return;
            }

            Relationship relationship = relationshipRepository.findByUser_IdxAndTargetUser_Idx(userId, targetUser.getIdx())
                    .orElse(null);

            if (relationship == null || relationship.getStatus() != RelationshipStatus.ACTIVE) {
                relationship = groupRelationshipService.createActiveRelationshipPair(userId, targetUser.getIdx());
            }

            addRecipient(recipientByUserId, relationship.getTargetUser(), "email");
        });

        return new RecipientResolution(
                new ArrayList<>(recipientByUserId.keySet()),
                new ArrayList<>(recipientByUserId.values()),
                pendingInvites
        );
    }

    private RecipientResolution resolveChatRecipients(Long userId, GroupShareDto.ChatShareRequest request) {
        User actor = getUser(userId);
        Map<Long, GroupShareDto.ShareRecipient> recipientByUserId = new LinkedHashMap<>();
        List<GroupShareDto.PendingInvite> pendingInvites = new ArrayList<>();

        List<Relationship> directRelationships = groupRelationshipService.getActiveRelationships(userId);
        Map<Long, Relationship> directRelationshipByUserId = new LinkedHashMap<>();
        directRelationships.forEach(relationship -> directRelationshipByUserId.put(
                relationship.getTargetUser().getIdx(),
                relationship
        ));

        Map<Long, RelationshipGroup> accessibleGroups = resolveAccessibleChatGroups(userId);
        Map<Long, GroupShareDto.ShareRecipient> accessibleGroupMemberByUserId = new LinkedHashMap<>();

        accessibleGroups.values().forEach(group ->
                buildChatGroupMembers(userId, group, directRelationshipByUserId).forEach(member -> {
                    GroupRelationshipDto.UserSummary targetUser = member.targetUser();
                    if (targetUser == null || targetUser.userId() == null || Objects.equals(targetUser.userId(), userId)) {
                        return;
                    }
                    accessibleGroupMemberByUserId.putIfAbsent(
                            targetUser.userId(),
                            new GroupShareDto.ShareRecipient(
                                    targetUser.userId(),
                                    targetUser.name(),
                                    targetUser.email(),
                                    "group-member"
                            )
                    );
                })
        );

        if (request.userIds() != null) {
            for (Long selectedUserId : request.userIds().stream().filter(Objects::nonNull).distinct().toList()) {
                if (Objects.equals(selectedUserId, userId)) {
                    continue;
                }

                Relationship directRelationship = directRelationshipByUserId.get(selectedUserId);
                if (directRelationship != null) {
                    addRecipient(recipientByUserId, directRelationship.getTargetUser(), "user");
                    continue;
                }

                GroupShareDto.ShareRecipient sharedMember = accessibleGroupMemberByUserId.get(selectedUserId);
                if (sharedMember != null) {
                    recipientByUserId.putIfAbsent(sharedMember.userId(), sharedMember);
                }
            }
        }

        normalizeGroupIds(request.groupIds()).forEach(groupId -> {
            RelationshipGroup group = accessibleGroups.get(groupId);
            if (group == null) {
                throw BaseException.from(GROUP_NOT_FOUND);
            }

            buildChatGroupMembers(userId, group, directRelationshipByUserId).forEach(member -> {
                GroupRelationshipDto.UserSummary targetUser = member.targetUser();
                if (targetUser == null || targetUser.userId() == null || Objects.equals(targetUser.userId(), userId)) {
                    return;
                }

                recipientByUserId.putIfAbsent(
                        targetUser.userId(),
                        new GroupShareDto.ShareRecipient(
                                targetUser.userId(),
                                targetUser.name(),
                                targetUser.email(),
                                "group:" + group.getName()
                        )
                );
            });
        });

        normalizeEmails(request.emails()).forEach(email -> {
            if (actor.getEmail() != null && actor.getEmail().equalsIgnoreCase(email)) {
                return;
            }

            User targetUser = userRepository.findByEmail(email).orElse(null);
            if (targetUser == null) {
                RelationshipInvite invite = groupRelationshipService.createPendingEmailInvite(userId, email, InviteType.CHAT);
                pendingInvites.add(new GroupShareDto.PendingInvite(invite.getId(), email, InviteType.CHAT));
                return;
            }

            Relationship relationship = relationshipRepository.findByUser_IdxAndTargetUser_Idx(userId, targetUser.getIdx())
                    .orElse(null);

            if (relationship == null || relationship.getStatus() != RelationshipStatus.ACTIVE) {
                relationship = groupRelationshipService.createActiveRelationshipPair(userId, targetUser.getIdx());
            }

            addRecipient(recipientByUserId, relationship.getTargetUser(), "email");
        });

        return new RecipientResolution(
                new ArrayList<>(recipientByUserId.keySet()),
                new ArrayList<>(recipientByUserId.values()),
                pendingInvites
        );
    }

    private Map<Long, RelationshipGroup> resolveAccessibleChatGroups(Long userId) {
        Map<Long, RelationshipGroup> groups = new LinkedHashMap<>();

        relationshipGroupRepository.findAllByUser_IdxOrderByCreatedAtAsc(userId)
                .forEach(group -> groups.put(group.getId(), group));

        relationshipGroupInviteRepository.findAllByToUser_IdxAndStatusOrderByCreatedAtDesc(userId, InviteStatus.ACCEPTED)
                .forEach(invite -> groups.putIfAbsent(invite.getGroup().getId(), invite.getGroup()));

        return groups;
    }

    private List<GroupRelationshipDto.RelationshipSummary> buildChatGroupMembers(
            Long actorUserId,
            RelationshipGroup group,
            Map<Long, Relationship> directRelationshipByUserId
    ) {
        Map<Long, GroupRelationshipDto.RelationshipSummary> memberByUserId = new LinkedHashMap<>();

        if (!Objects.equals(group.getUser().getIdx(), actorUserId)) {
            memberByUserId.put(
                    group.getUser().getIdx(),
                    createChatRelationshipSummary(directRelationshipByUserId.get(group.getUser().getIdx()), group.getUser(), group)
            );
        }

        relationshipGroupMappingRepository.findAllByGroup_Id(group.getId()).stream()
                .map(RelationshipGroupMapping::getRelationship)
                .filter(Objects::nonNull)
                .filter(relationship -> relationship.getStatus() == RelationshipStatus.ACTIVE)
                .map(Relationship::getTargetUser)
                .filter(Objects::nonNull)
                .filter(targetUser -> !Objects.equals(targetUser.getIdx(), actorUserId))
                .forEach(targetUser -> memberByUserId.putIfAbsent(
                        targetUser.getIdx(),
                        createChatRelationshipSummary(directRelationshipByUserId.get(targetUser.getIdx()), targetUser, group)
                ));

        return new ArrayList<>(memberByUserId.values());
    }

    private GroupRelationshipDto.RelationshipSummary createChatRelationshipSummary(
            Relationship directRelationship,
            User targetUser,
            RelationshipGroup group
    ) {
        return new GroupRelationshipDto.RelationshipSummary(
                directRelationship != null ? directRelationship.getId() : targetUser.getIdx(),
                GroupRelationshipDto.UserSummary.from(targetUser),
                directRelationship != null ? directRelationship.getStatus().name() : RelationshipStatus.ACTIVE.name(),
                directRelationship != null ? directRelationship.getCreatedAt() : group.getCreatedAt(),
                List.of(GroupRelationshipDto.GroupTag.from(group))
        );
    }

    private void addRecipient(Map<Long, GroupShareDto.ShareRecipient> recipientByUserId, User user, String source) {
        recipientByUserId.putIfAbsent(
                user.getIdx(),
                new GroupShareDto.ShareRecipient(user.getIdx(), user.getName(), user.getEmail(), source)
        );
    }

    private List<Long> normalizeGroupIds(List<Long> groupIds) {
        return groupIds == null
                ? List.of()
                : groupIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    private List<String> normalizeEmails(List<String> emails) {
        return emails == null
                ? List.of()
                : emails.stream()
                .filter(Objects::nonNull)
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .distinct()
                .toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> BaseException.from(USER_NOT_FOUND));
    }

    private record RecipientResolution(
            List<Long> userIds,
            List<GroupShareDto.ShareRecipient> recipients,
            List<GroupShareDto.PendingInvite> pendingInvites
    ) {
    }
}
