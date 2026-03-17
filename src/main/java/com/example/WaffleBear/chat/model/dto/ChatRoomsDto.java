package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.ChatMessageRepository;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
    public static class PageRes {
        private List<ListRes> boardList;
        private int totalPage;
        private long totalCount;

        public static PageRes from(Page<ChatParticipants> result, Map<Long, Long> unreadMap) {
            return PageRes.builder()
                    .boardList(result.getContent().stream()
                            .map(p -> {
                                long unread = unreadMap.getOrDefault(p.getChatRooms().getIdx(), 0L);
                                return ListRes.from(p, unread);
                            })
                            .toList())
                    .totalPage(result.getTotalPages())
                    .totalCount(result.getTotalElements())
                    .build();
        }
    }

    @Builder
    @Getter
    public static class ListRes {
        private Long idx;
        private String title;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private int participantCount;
        private long unreadCount;

        // ChatParticipants 엔티티를 전달받아 데이터를 가공합니다.
        public static ListRes from(ChatParticipants participant, long unreadCount) {
            ChatRooms room = participant.getChatRooms();

            // 1. 개인 설정 이름이 있으면 사용, 없으면 방의 기본 제목 사용
            String displayName = (participant.getCustomRoomName() != null && !participant.getCustomRoomName().isEmpty())
                    ? participant.getCustomRoomName()
                    : room.getTitle();

            return ListRes.builder()
                    .idx(room.getIdx())
                    .title(displayName) // 사용자별로 다른 이름이 할당됩니다.
                    .lastMessage(room.getLastMessage())
                    .lastMessageTime(room.getLastMessageTime())
                    .participantCount(room.getParticipants() != null ? room.getParticipants().size() : 0)
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
