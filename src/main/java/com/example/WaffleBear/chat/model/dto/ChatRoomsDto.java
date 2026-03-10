package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomsDto {
    @Getter
    public static class ChatRoomsReq {
        private List<Long> participantsIdx;

        // Service로부터 이미 조회된 User 리스트를 전달받아 처리
        public ChatRooms toEntity() {
            return ChatRooms.builder()
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // 특정 유저와 방을 연결하는 중간 엔티티 생성 로직
        public ChatParticipants toParticipantEntity(ChatRooms room, User user) {
            return ChatParticipants.builder()
                    .chatRooms(room)
                    .users(user)
                    .customRoomName(room.getTitle()) // 혹은 DTO에서 받은 이름
                    .build();
        }
    }
}
