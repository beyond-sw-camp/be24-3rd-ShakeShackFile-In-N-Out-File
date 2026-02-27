package com.example.WaffleBear.workspace;

import com.example.WaffleBear.workspace.model.post.Posts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Posts, Long> {

    Optional<Posts> findByUser_idx(Long userIdx);

    List<Posts> findAllByUser_idx(Long userIdx);
}
