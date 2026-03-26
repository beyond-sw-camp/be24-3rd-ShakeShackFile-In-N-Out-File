package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ChatParticipantsDto {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userIdx;
        private String nickname;
        private String customRoomName;
        private LocalDateTime joinedAt;
        private boolean isFavorite;

        // Entity -> DTO 변환
        public static Response from(ChatParticipants participant) {
            return Response.builder()
                    .id(participant.getIdx())
                    .userIdx(participant.getUsers().getIdx()) // User 엔티티의 ID
                    .nickname(participant.getUsers().getName())
                    .customRoomName(participant.getCustomRoomName())
                    .joinedAt(participant.getJoinedAt())
                    .isFavorite(participant.isFavorite())
                    .build();
        }
    }

    public static class Create {
        public static ChatParticipants toEntity(ChatRooms room, User user) {
            return ChatParticipants.builder()
                    .chatRooms(room)
                    .users(user)
                    .joinedAt(LocalDateTime.now())
                    .lastReadMessageId(0L)
                    .isFavorite(false)
                    .build();
        }
    }
}
