package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipantsRepository extends JpaRepository<ChatParticipants,Long> {
     boolean existsByChatRoomsIdxAndUsersIdx(Long roomId, Long userIdx);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatParticipants p WHERE p.chatRooms.idx = :roomIdx AND p.users.idx = :userIdx")
    void deleteByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);

    boolean existsByChatRoomsIdx(Long roomIdx);

    @Query("""
    SELECT cp
    FROM ChatParticipants cp
    JOIN cp.chatRooms cr
    WHERE cp.users.idx = :userIdx
    ORDER BY cr.lastMessageTime DESC NULLS LAST
""")
    Page<ChatParticipants> findAllByUsersIdx(Long userIdx, Pageable pageable);

    List<ChatParticipants> findAllByUsersIdx(Long userIdx);
    Optional<ChatParticipants> findByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);

    @Query("select cp from ChatParticipants cp join fetch cp.users u where cp.chatRooms.idx = :roomIdx")
    List<ChatParticipants> findAllByChatRoomsIdx(Long roomIdx);

    public interface RoomParticipantCountView {
        Long getRoomIdx();
        Long getParticipantCount();
    }

    @Query("""
    SELECT cp.chatRooms.idx as roomIdx, COUNT(cp) as participantCount
    FROM ChatParticipants cp
    WHERE cp.chatRooms.idx IN :roomIds
    GROUP BY cp.chatRooms.idx
""")
    List<RoomParticipantCountView> countParticipantsByRoomIds(@Param("roomIds") Collection<Long> roomIds);

}
