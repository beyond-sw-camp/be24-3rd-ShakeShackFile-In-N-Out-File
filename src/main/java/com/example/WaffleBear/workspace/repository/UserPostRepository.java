package com.example.WaffleBear.workspace.repository;

import com.example.WaffleBear.workspace.model.relation.UserPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPostRepository extends JpaRepository<UserPost, Long> {
}
