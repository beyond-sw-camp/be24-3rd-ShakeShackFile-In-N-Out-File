package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantsRepository extends JpaRepository<ChatParticipants,Long> {
     boolean existsByChatRoomsIdxAndUsersIdx(Long roomId, Long userIdx);

    void deleteByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);

    boolean existsByChatRoomsIdx(Long roomIdx);

    Page<ChatParticipants> findAllByUsersIdx(Long userIdx, PageRequest pageRequest);

    Optional<ChatParticipants> findByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);
}
