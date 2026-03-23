package com.example.WaffleBear.user.repository;

import com.example.WaffleBear.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);
    Optional<User> findByName(String name);
    Optional<User> findByIdx(Long idx);
}
