package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantsRepository extends JpaRepository<ChatParticipants,Long> {
    boolean existsByChatRoomsAndUsersIdx(ChatRooms room, Long userIdx);

    void deleteByChatRoomsIdxAndUsersIdx(Long roomIdx, Long userIdx);

    boolean existsByChatRoomsIdx(Long roomIdx);
}
