package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessages, Long> {

    Page<ChatMessages> findAllByChatRooms(ChatRooms room, Pageable pageable);
    Optional<ChatMessages> findTopByChatRoomsIdxOrderByCreatedAtDesc(Long roomIdx);
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
}
