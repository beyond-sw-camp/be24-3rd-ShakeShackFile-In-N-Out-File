package com.example.WaffleBear.group.repository;

import com.example.WaffleBear.group.model.entity.RelationshipGroupMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RelationshipGroupMappingRepository extends JpaRepository<RelationshipGroupMapping, Long> {

    @EntityGraph(attributePaths = {"relationship", "relationship.targetUser", "group"})
    List<RelationshipGroupMapping> findAllByRelationship_User_Idx(Long userId);

    @EntityGraph(attributePaths = {"relationship", "relationship.targetUser", "group"})
    List<RelationshipGroupMapping> findAllByRelationship_Id(Long relationshipId);

    @EntityGraph(attributePaths = {"relationship", "relationship.targetUser", "group"})
    List<RelationshipGroupMapping> findAllByRelationship_IdIn(Collection<Long> relationshipIds);

    @EntityGraph(attributePaths = {"relationship", "relationship.targetUser", "group"})
    List<RelationshipGroupMapping> findAllByGroup_Id(Long groupId);

    boolean existsByRelationship_IdAndGroup_Id(Long relationshipId, Long groupId);

    void deleteByRelationship_IdAndGroup_Id(Long relationshipId, Long groupId);

    void deleteAllByRelationship_Id(Long relationshipId);

    void deleteAllByGroup_Id(Long groupId);
}
