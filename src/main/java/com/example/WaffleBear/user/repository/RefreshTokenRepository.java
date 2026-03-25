package com.example.WaffleBear.user.repository;
import com.example.WaffleBear.user.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.email = :email")
    Optional<RefreshToken> findByEmailForUpdate(@Param("email") String email);

    void deleteByEmail(String email);
    void deleteByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime now);
}