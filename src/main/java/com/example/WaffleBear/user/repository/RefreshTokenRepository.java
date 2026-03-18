package com.example.WaffleBear.user.repository;
import com.example.WaffleBear.user.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByEmail(String email);
    void deleteByEmail(String email);
    void deleteByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime now);
}