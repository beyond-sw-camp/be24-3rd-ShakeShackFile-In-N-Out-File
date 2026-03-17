package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatRooms;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRooms,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ChatRooms r WHERE r.idx = :idx")
    Optional<ChatRooms> findByIdWithLock(@Param("idx") Long idx);
}
