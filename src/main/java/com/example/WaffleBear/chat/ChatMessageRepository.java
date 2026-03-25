package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
            "AND p.users.idx != :senderIdx")
    int countUnreadParticipants(
            @Param("roomIdx") Long roomIdx,
            @Param("messageIdx") Long messageIdx,
            @Param("senderIdx") Long senderIdx
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatMessages m WHERE m.chatRooms.idx = :roomIdx")
    void deleteAllByChatRoomsIdx(Long roomIdx);

    @EntityGraph(attributePaths = "sender")
    Page<ChatMessages> findAllByChatRoomsAndCreatedAtAfter(ChatRooms room, LocalDateTime joinedAt, Pageable pageable);

    long countByChatRoomsIdxAndIdxGreaterThanAndCreatedAtAfter(
            Long chatRoomsIdx, Long idx, LocalDateTime createdAt
    );

    Optional<ChatMessages> findTopByChatRoomsIdxAndCreatedAtAfterOrderByCreatedAtDesc(
            Long chatRoomsIdx, LocalDateTime after
    );

    @EntityGraph(attributePaths = "sender")
    Optional<ChatMessages> findByIdxAndChatRoomsIdx(Long messageIdx, Long roomIdx);

    @Query("SELECT m.idx, COUNT(p) FROM ChatMessages m, ChatParticipants p " +
            "WHERE m.chatRooms.idx = :roomIdx " +
            "AND m.idx IN :messageIds " +
            "AND p.chatRooms.idx = :roomIdx " +
            "AND (p.lastReadMessageId IS NULL OR p.lastReadMessageId < m.idx) " +
            "AND p.users.idx <> m.sender.idx " +
            "GROUP BY m.idx")
    List<Object[]> countUnreadParticipantsByMessageIds(
            @Param("roomIdx") Long roomIdx,
            @Param("messageIds") Collection<Long> messageIds
    );
}
