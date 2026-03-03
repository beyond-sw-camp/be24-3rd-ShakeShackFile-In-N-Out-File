package com.example.WaffleBear.workspace.repository;

import com.example.WaffleBear.workspace.model.post.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Workspace, Long> {

    @Query("SELECT p FROM Workspace p JOIN p.userPosts up WHERE up.user.idx = :userIdx")
    List<Workspace> findAllByUserId(@Param("userIdx") Long userIdx);

    // 만약 단건 조회가 필요하다면
    @Query("SELECT p FROM Workspace p JOIN p.userPosts up WHERE up.user.idx = :userIdx AND p.idx = :postIdx")
    Optional<Workspace> findByPostIdAndUserId(@Param("postIdx") Long postIdx, @Param("userIdx") Long userIdx);
}
