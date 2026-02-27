package com.example.WaffleBear.workspace.repository;

import com.example.WaffleBear.workspace.model.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByUser_idx(Long userIdx);

    List<Post> findAllByUser_idx(Long userIdx);
}
