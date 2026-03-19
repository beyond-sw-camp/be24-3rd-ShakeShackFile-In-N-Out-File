package com.example.WaffleBear.group.repository;

import com.example.WaffleBear.group.model.entity.RelationshipGroupInvite;
import com.example.WaffleBear.group.model.enums.InviteStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelationshipGroupInviteRepository extends JpaRepository<RelationshipGroupInvite, Long> {

    @EntityGraph(attributePaths = {"group", "group.user", "fromUser", "toUser"})
    List<RelationshipGroupInvite> findAllByToUser_IdxOrderByCreatedAtDesc(Long toUserId);

    @EntityGraph(attributePaths = {"group", "group.user", "fromUser", "toUser"})
    List<RelationshipGroupInvite> findAllByFromUser_IdxOrderByCreatedAtDesc(Long fromUserId);

    @EntityGraph(attributePaths = {"group", "group.user", "fromUser", "toUser"})
    List<RelationshipGroupInvite> findAllByToUser_IdxAndStatusOrderByCreatedAtDesc(Long toUserId, InviteStatus status);

    @EntityGraph(attributePaths = {"group", "group.user", "fromUser", "toUser"})
    Optional<RelationshipGroupInvite> findById(Long id);

    Optional<RelationshipGroupInvite> findByGroup_IdAndToUser_IdxAndStatus(Long groupId, Long toUserId, InviteStatus status);
}
