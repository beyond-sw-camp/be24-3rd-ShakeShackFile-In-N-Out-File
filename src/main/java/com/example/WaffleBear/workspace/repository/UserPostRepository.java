package com.example.WaffleBear.workspace.repository;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPostRepository extends JpaRepository<UserPost, Long> {
    @Query("SELECT up FROM UserPost up JOIN FETCH up.workspace WHERE up.user.idx = :userId AND up.workspace.idx = :workspaceId")
    Optional<UserPost> findByUser_IdxAndWorkspace_Idx(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);

    @Query("SELECT up FROM UserPost up JOIN FETCH up.user WHERE up.workspace.idx = :postIdx")
    List<UserPost> findAllByWorkspace_idx(@Param("postIdx") Long postIdx);

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

    // 방법 1: 쿼리 메소드 (엔티티 구조에 따라 이름이 달라질 수 있음)
    // UserPost 엔티티 안에 Post 객체가 있고 그 안에 idx가 있는 경우
//    List<UserPost> findAllByPostIdx(Long postIdx);

    // 방법 2: @Query 어노테이션 활용 (성능 및 정확도 면에서 추천)
    // 필요한 User ID만 Long 타입으로 바로 뽑아옵니다.
    @Query("SELECT up.user.idx FROM UserPost up WHERE up.workspace.idx = :postIdx")
    List<Long> findUserIdsByPostIdx(@Param("postIdx") Long postIdx);
}
