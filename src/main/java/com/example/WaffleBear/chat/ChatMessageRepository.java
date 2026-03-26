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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessages, Long> {

    interface RoomUnreadCountView {
        Long getRoomIdx();
        Long getUnreadCount();
    }

    interface MessageUnreadCountView {
        Long getMessageIdx();
        Long getUnreadCount();
    }

    interface UserStoredBytesProjection {
        Long getUserIdx();
        Long getStoredBytes();
    }

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

    @Query("""
            SELECT cp.chatRooms.idx as roomIdx, COUNT(m) as unreadCount
            FROM ChatParticipants cp
            LEFT JOIN ChatMessages m
              ON m.chatRooms.idx = cp.chatRooms.idx
             AND m.idx > COALESCE(cp.lastReadMessageId, 0)
             AND m.createdAt > COALESCE(cp.joinedAt, :defaultJoinedAt)
            WHERE cp.users.idx = :userIdx
              AND cp.chatRooms.idx IN :roomIds
            GROUP BY cp.chatRooms.idx
            """)
    List<RoomUnreadCountView> countUnreadCountsByUserAndRoomIds(
            @Param("userIdx") Long userIdx,
            @Param("roomIds") Collection<Long> roomIds,
            @Param("defaultJoinedAt") LocalDateTime defaultJoinedAt
    );

    @Query("""
            SELECT m.idx as messageIdx, COUNT(p) as unreadCount
            FROM ChatMessages m
            LEFT JOIN ChatParticipants p
              ON p.chatRooms.idx = m.chatRooms.idx
             AND (p.lastReadMessageId IS NULL OR p.lastReadMessageId < m.idx)
             AND p.users.idx <> m.sender.idx
            WHERE m.idx IN :messageIds
            GROUP BY m.idx
            """)
    List<MessageUnreadCountView> countUnreadParticipantsByMessageIds(
            @Param("messageIds") Collection<Long> messageIds
    );

    Optional<ChatMessages> findByIdxAndChatRoomsIdx(Long messageIdx, Long roomIdx);

    @Query("""
            select
                m.sender.idx as userIdx,
                sum(coalesce(m.fileSize, 0)) as storedBytes
            from ChatMessages m
            where m.sender.idx is not null
            group by m.sender.idx
            """)
    List<UserStoredBytesProjection> aggregateStoredBytesBySender();
}
