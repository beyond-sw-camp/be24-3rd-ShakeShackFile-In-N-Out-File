package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessages, Long> {

    Page<ChatMessages> findAllByChatRooms(ChatRooms room, Pageable pageable);
    Optional<ChatMessages> findTopByChatRoomsIdxOrderByCreatedAtDesc(Long roomIdx);
    Page<ChatMessages> findByChatRoomsIdxAndCreatedAtAfterOrderByCreatedAtAsc(
            Long roomIdx, LocalDateTime after, Pageable pageable
    );
    long countByChatRoomsIdxAndIdxGreaterThan(Long roomIdx, Long lastReadMessageId);

    @Query("SELECT COUNT(p) FROM ChatParticipants p " +
            "WHERE p.chatRooms.idx = :roomIdx " +
            "AND (p.lastReadMessageId IS NULL OR p.lastReadMessageId < :messageIdx) " +
            "AND p.users.idx != :senderIdx") // 발신자 제외
    int countUnreadParticipants(
            @Param("roomIdx") Long roomIdx,
            @Param("messageIdx") Long messageIdx,
            @Param("senderIdx") Long senderIdx
    );
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatMessages m WHERE m.chatRooms.idx = :roomIdx")
    void deleteAllByChatRoomsIdx(Long roomIdx);

    Page<ChatMessages> findAllByChatRoomsAndCreatedAtAfter(ChatRooms room, LocalDateTime joinedAt, Pageable pageable);
    long countByChatRoomsIdxAndIdxGreaterThanAndCreatedAtAfter(
            Long chatRoomsIdx, Long idx, LocalDateTime createdAt
    );
    Optional<ChatMessages> findTopByChatRoomsIdxAndCreatedAtAfterOrderByCreatedAtDesc(
            Long chatRoomsIdx, LocalDateTime after
    );
}
