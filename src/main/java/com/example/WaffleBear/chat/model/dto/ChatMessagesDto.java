package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;


import java.time.LocalDateTime;
import java.util.List;

public class ChatMessagesDto {
    @Getter
    public static class Send {
        private String contents;
        // senderIdx는 보통 @AuthenticationPrincipal에서 가져오거나
        // 웹소켓 연결 시 인증 정보를 통해 처리하지만, DTO에 포함할 수도 있습니다.

        public ChatMessages toEntity(ChatRooms room, User sender) {
            return ChatMessages.builder()
                    .chatRooms(room)
                    .sender(sender)
                    .contents(this.contents)
                    .sendTime(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PageRes {
        private List<ListRes> messageList;
        private int totalPage;
        private long totalCount;
        private int currentPage;
        private int currentSize;

        public static PageRes from(Page<ChatMessages> result) {
            return PageRes.builder()
                    .messageList(result.get().map(ChatMessagesDto.ListRes::from).toList())
                    .totalPage(result.getTotalPages())
                    .totalCount(result.getTotalElements())
                    .currentPage(result.getPageable().getPageNumber())
                    .currentSize(result.getPageable().getPageSize())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ListRes {
        private Long idx;
        private Long senderIdx;
        private String senderNickname;
        private String contents;
        private LocalDateTime createdAt;

        public static ListRes from(ChatMessages entity) {
            return ListRes.builder()
                    .idx(entity.getIdx())
                    .senderIdx(entity.getSender().getIdx())
                    .senderNickname(entity.getSender().getName()) // User 엔티티에 nickname이 있다고 가정
                    .contents(entity.getContents())
                    .createdAt(entity.getSendTime())
                    .build();
        }
    }
}
