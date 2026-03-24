package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ChatParticipantsDto {

    @Schema(description = "채팅 참여자 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        @Schema(description = "참여자 ID", example = "1")
        private Long id;
        @Schema(description = "사용자 IDX", example = "5")
        private Long userIdx;
        @Schema(description = "사용자 닉네임", example = "홍길동")
        private String nickname;
        @Schema(description = "채팅방 커스텀 이름", example = "우리팀 회의방")
        private String customRoomName;
        @Schema(description = "참여 일시")
        private LocalDateTime joinedAt;
        @Schema(description = "즐겨찾기 여부", example = "false")
        private boolean isFavorite;

        public static Response from(ChatParticipants participant) {
            return Response.builder()
                    .id(participant.getIdx())
                    .userIdx(participant.getUsers().getIdx())
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
