package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipantsRepository extends JpaRepository<ChatParticipants,Long> {
     boolean existsByChatRoomsIdxAndUsersIdx(Long roomId, Long userIdx);

    void deleteByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);

    boolean existsByChatRoomsIdx(Long roomIdx);

    @Query("SELECT cp FROM ChatParticipants cp JOIN cp.chatRooms cr WHERE cp.users.idx = :userIdx ORDER BY cr.lastMessageTime DESC NULLS LAST")
    Page<ChatParticipants> findAllByUsersIdx(Long userIdx, PageRequest pageRequest);

    List<ChatParticipants> findAllByUsersIdx(Long userIdx);
    Optional<ChatParticipants> findByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);

    List<ChatParticipants> findAllByChatRoomsIdx(Long roomIdx);
}
