package com.example.WaffleBear.workspace.repository;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPostRepository extends JpaRepository<UserPost, Long> {
    // 1. 유저 ID와 워크스페이스 ID를 동시에 만족하는 유일한 데이터를 조회
    Optional<UserPost> findByUser_IdxAndWorkspace_Idx(Long userId, Long workspaceId);

    // 또는 2. 해당 유저의 모든 권한 리스트를 가져옴
    List<UserPost> findAllByWorkspace_idx(Long post_idx);

    // 해당하는 포스트의 모든 유저들 가져옴
    @Query("SELECT up FROM UserPost up " +
            "WHERE up.workspace.idx = :workspaceId " + // 필드명이 'workspace'이므로 up.workspace.idx로 수정
            "AND up.user.idx IN :userIds " +
            "AND up.user.idx != :adminId")
    List<UserPost> findAllByWorkspaceIdAndUserIdsExceptAdmin(
            @Param("userIds") List<Long> userIds,
            @Param("workspaceId") Long workspaceId,
            @Param("adminId") Long adminId
    );

    Optional<UserPost> findByUser(User user);
}
