package com.example.WaffleBear.workspace.repository;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPostRepository extends JpaRepository<UserPost, Long> {
    Optional<UserPost> findByUser_IdxAndWorkspace_Idx(Long userId, Long workspaceId);

    List<UserPost> findAllByWorkspace_idx(Long post_idx);

    @Query("SELECT up FROM UserPost up JOIN FETCH up.workspace w WHERE up.user.idx = :userIdx ORDER BY w.updatedAt DESC, w.createdAt DESC")
    List<UserPost> findAllByUser_IdxOrderByWorkspaceUpdatedAtDesc(@Param("userIdx") Long userIdx);

    Optional<UserPost> deleteByUser_IdxAndWorkspace_Idx(Long userId, Long workspaceId);

    @Query("SELECT up FROM UserPost up " +
            "WHERE up.workspace.idx = :workspaceId " +
            "AND up.user.idx IN :userIds " +
            "AND up.user.idx != :adminId")
    List<UserPost> findAllByWorkspaceIdAndUserIdsExceptAdmin(
            @Param("userIds") List<Long> userIds,
            @Param("workspaceId") Long workspaceId,
            @Param("adminId") Long adminId
    );

    Optional<UserPost> findByUser(User user);
}
