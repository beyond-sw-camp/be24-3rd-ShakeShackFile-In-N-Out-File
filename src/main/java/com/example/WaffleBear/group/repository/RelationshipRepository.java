package com.example.WaffleBear.group.repository;

import com.example.WaffleBear.group.model.entity.Relationship;
import com.example.WaffleBear.group.model.enums.RelationshipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    @EntityGraph(attributePaths = {"user", "targetUser"})
    List<Relationship> findAllByUser_IdxOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "targetUser"})
    List<Relationship> findAllByUser_IdxAndStatusOrderByCreatedAtDesc(Long userId, RelationshipStatus status);

    @EntityGraph(attributePaths = {"user", "targetUser"})
    List<Relationship> findAllByUser_IdxAndTargetUser_IdxIn(Long userId, Collection<Long> targetUserIds);

    @EntityGraph(attributePaths = {"user", "targetUser"})
    Optional<Relationship> findById(Long id);

    Optional<Relationship> findByUser_IdxAndTargetUser_Idx(Long userId, Long targetUserId);
}
