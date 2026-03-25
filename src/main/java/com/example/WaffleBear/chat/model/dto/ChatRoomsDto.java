package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.ChatMessageRepository;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ChatRoomsDto {

    @Schema(description = "채팅방 생성 요청")
    @Getter
    public static class ChatRoomsReq {
        @Schema(description = "채팅방 제목", example = "프로젝트 회의방")
        private String title;
        @Schema(description = "초대할 사용자 이메일 목록", example = "[\"user1@example.com\", \"user2@example.com\"]")
        @NotEmpty(message = "초대할 유저를 입력해주세요.")
        private List<String> participantsEmail;

        public ChatRooms toEntity() {
            return ChatRooms.builder()
                    .title(this.title)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        public ChatParticipants toParticipantEntity(ChatRooms room, User user) {
            return ChatParticipants.builder()
                    .chatRooms(room)
                    .users(user)
                    .customRoomName(room.getTitle())
                    .build();
        }
    }

    @Schema(description = "채팅방 페이지 응답")
    @Getter
    @Builder
    public static class PageRes {
        @Schema(description = "채팅방 목록")
        private List<ListRes> boardList;
        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPage;
        @Schema(description = "전체 채팅방 수", example = "50")
        private long totalCount;

        public static PageRes from(Page<ChatParticipants> result,
                                   Map<Long, Long> unreadMap,
                                   Map<Long, String> lastMessageMap,
                                   Map<Long, Integer> participantCountMap) {
            return PageRes.builder()
                    .boardList(result.getContent().stream()
                            .map(p -> {
                                Long roomIdx = p.getChatRooms().getIdx();
                                long unread = unreadMap.getOrDefault(roomIdx, 0L);
                                String lastMessage = lastMessageMap.getOrDefault(roomIdx, "");
                                int participantCount = participantCountMap.getOrDefault(roomIdx, 0);
                                return ListRes.from(p, unread, lastMessage, participantCount);
                            })
                            .toList())
                    .totalPage(result.getTotalPages())
                    .totalCount(result.getTotalElements())
                    .build();
        }
    }

    @Schema(description = "채팅방 목록 항목")
    @Builder
    @Getter
    public static class ListRes {
        @Schema(description = "채팅방 ID", example = "1")
        private Long idx;
        @Schema(description = "채팅방 제목", example = "프로젝트 회의방")
        private String title;
        @Schema(description = "마지막 메시지 내용", example = "안녕하세요!")
        private String lastMessage;
        @Schema(description = "마지막 메시지 시간")
        private LocalDateTime lastMessageTime;
        @Schema(description = "참여자 수", example = "5")
        private int participantCount;
        @Schema(description = "읽지 않은 메시지 수", example = "3")
        private long unreadCount;

        public static ListRes from(ChatParticipants participant,
                                   long unreadCount,
                                   String lastMessage,
                                   int participantCount) {
            ChatRooms room = participant.getChatRooms();
            String displayName = (participant.getCustomRoomName() != null && !participant.getCustomRoomName().isEmpty())
                    ? participant.getCustomRoomName()
                    : room.getTitle();

            return ListRes.builder()
                    .idx(room.getIdx())
                    .title(displayName)
                    .lastMessage(lastMessage)
                    .lastMessageTime(room.getLastMessageTime())
                    .participantCount(participantCount)
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    @Schema(description = "채팅방 제목 변경 요청")
    @Getter
    @Setter
    public static class UpdateTitleReq {
        @Schema(description = "변경할 채팅방 제목", example = "새 회의방 이름")
        private String title;
    }

}
