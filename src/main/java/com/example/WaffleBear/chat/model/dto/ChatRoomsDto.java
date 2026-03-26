package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ChatRoomsDto {
    @Getter
    public static class ChatRoomsReq {
        private String title;
        // 최소 1명 이상은 초대해야 함을 명시
        @NotEmpty(message = "초대할 유저를 입력해주세요.")
        private List<String> participantsEmail;

        // Service로부터 이미 조회된 User 리스트를 전달받아 처리
        public ChatRooms toEntity() {
            return ChatRooms.builder()
                    .title(this.title)
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
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageRes {
        private List<ListRes> boardList;
        private int totalPage;
        private long totalCount;

        public static PageRes from(
                Page<ChatParticipants> result,
                Map<Long, Long> unreadMap,
                Map<Long, String> lastMessageMap,
                Map<Long, Long> participantCountMap
        ) {
            return PageRes.builder()
                    .boardList(result.getContent().stream()
                            .map(p -> {
                                long unread = unreadMap.getOrDefault(p.getChatRooms().getIdx(), 0L);
                                String lastMessage = lastMessageMap.getOrDefault(p.getChatRooms().getIdx(), "");
                                long participantCount = participantCountMap.getOrDefault(p.getChatRooms().getIdx(), 0L);
                                return ListRes.from(p, unread, lastMessage, participantCount);
                            })
                            .toList())
                    .totalPage(result.getTotalPages())
                    .totalCount(result.getTotalElements())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListRes {
        private Long idx;
        private String title;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private int participantCount;
        private long unreadCount;

        // ChatParticipants 엔티티를 전달받아 데이터를 가공합니다.
        public static ListRes from(
                ChatParticipants participant,
                long unreadCount,
                String lastMessage,
                long participantCount
        ) {
            ChatRooms room = participant.getChatRooms();
            String displayName = (participant.getCustomRoomName() != null && !participant.getCustomRoomName().isEmpty())
                    ? participant.getCustomRoomName()
                    : room.getTitle();

            return ListRes.builder()
                    .idx(room.getIdx())
                    .title(displayName)
                    .lastMessage(lastMessage)
                    .lastMessageTime(room.getLastMessageTime())
                    .participantCount((int) participantCount)
                    .unreadCount(unreadCount)
                    .build();
        }
    }
    @Getter
    @Setter
    public static class UpdateTitleReq {
        private String title;
    }

}
