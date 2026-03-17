package com.example.WaffleBear.email;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {
    Optional<EmailVerify> findByToken(String token);

    Optional<EmailVerify> findByEmail(String email);

    Optional<EmailVerify> deleteByToken(String uuid);
}
