package com.example.WaffleBear.user;

import com.example.WaffleBear.user.model.EmailVerify;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {
    Optional<EmailVerify> findByToken(String token);
}
