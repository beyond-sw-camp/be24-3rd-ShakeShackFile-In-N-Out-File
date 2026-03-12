package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomsDto {
    @Getter
    public static class ChatRoomsReq {
        private String title;
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
    @Getter
    @Builder
    public static class PageRes {
        private List<ListRes> boardList;
        private int totalPage;
        private long totalCount;
        private int currentPage;
        private int currentSize;

        public static PageRes from(Page<ChatRooms> result) {
            return PageRes.builder()
                    .boardList(result.get().map(ChatRoomsDto.ListRes::from).toList())
                    .totalPage(result.getTotalPages())
                    .totalCount(result.getTotalElements())
                    .currentPage(result.getPageable().getPageNumber())
                    .currentSize(result.getPageable().getPageSize())
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

        public static ListRes from(ChatRooms entity) {
            return ListRes.builder()
                    .idx(entity.getIdx())
                    .title(entity.getTitle())
                    .lastMessage(entity.getLastMessage())
                    .lastMessageTime(entity.getLastMessageTime())
                    .build();
        }
    }

}
