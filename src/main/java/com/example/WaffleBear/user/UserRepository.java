package com.example.WaffleBear.user;

import com.example.WaffleBear.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
