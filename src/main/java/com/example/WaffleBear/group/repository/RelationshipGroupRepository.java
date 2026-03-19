package com.example.WaffleBear.group.repository;

import com.example.WaffleBear.group.model.entity.RelationshipGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelationshipGroupRepository extends JpaRepository<RelationshipGroup, Long> {

    List<RelationshipGroup> findAllByUser_IdxOrderByCreatedAtAsc(Long userId);

    Optional<RelationshipGroup> findByIdAndUser_Idx(Long groupId, Long userId);

    boolean existsByUser_IdxAndNameIgnoreCase(Long userId, String name);
}
