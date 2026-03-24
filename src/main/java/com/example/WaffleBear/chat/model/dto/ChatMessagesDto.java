package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.chat.model.entity.MessageType;
import com.example.WaffleBear.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;


import java.time.LocalDateTime;
import java.util.List;

public class ChatMessagesDto {

    @Schema(description = "채팅 메시지 전송 요청")
    @Getter
    public static class Send {
        @Schema(description = "메시지 내용", example = "안녕하세요!")
        private String contents;
        @Schema(description = "첨부 파일 URL")
        private String fileUrl;
        @Schema(description = "첨부 파일명", example = "document.pdf")
        private String fileName;
        @Schema(description = "첨부 파일 유형", example = "application/pdf")
        private String fileType;
        @Schema(description = "첨부 파일 크기 (바이트)", example = "1048576")
        private Long fileSize;
        @Schema(description = "메시지 유형 (TEXT, FILE, IMAGE 등)", example = "TEXT")
        private MessageType messageType;

        public ChatMessages toEntity(ChatRooms room, User sender) {
            return ChatMessages.builder()
                    .chatRooms(room)
                    .sender(sender)
                    .contents(this.contents)
                    .fileUrl(this.fileUrl)
                    .fileName(this.fileName)
                    .fileType(this.fileType)
                    .fileSize(this.fileSize)
                    .messageType(this.messageType != null ? this.messageType : MessageType.TEXT)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    @Schema(description = "채팅 메시지 페이지 응답")
    @Getter
    @Builder
    public static class PageRes {
        @Schema(description = "메시지 목록")
        private List<ListRes> messageList;
        @Schema(description = "전체 페이지 수", example = "10")
        private int totalPage;
        @Schema(description = "전체 메시지 수", example = "200")
        private long totalCount;
        @Schema(description = "현재 페이지 번호", example = "0")
        private int currentPage;
        @Schema(description = "페이지당 메시지 수", example = "20")
        private int currentSize;

        public static PageRes from(Page<ChatMessages> result) {
            return PageRes.builder()
                    .messageList(result.get().map(ListRes::from).toList())
                    .totalPage(result.getTotalPages())
                    .totalCount(result.getTotalElements())
                    .currentPage(result.getPageable().getPageNumber())
                    .currentSize(result.getPageable().getPageSize())
                    .build();
        }
    }

    @Schema(description = "채팅 메시지 항목")
    @Getter
    @Builder
    public static class ListRes {
        @Schema(description = "메시지 ID", example = "1")
        private Long idx;
        @Schema(description = "발신자 IDX", example = "5")
        private Long senderIdx;
        @Schema(description = "발신자 닉네임", example = "홍길동")
        private String senderNickname;
        @Schema(description = "메시지 내용", example = "안녕하세요!")
        private String contents;
        @Schema(description = "메시지 생성 일시")
        private LocalDateTime createdAt;
        @Schema(description = "읽지 않은 사용자 수", example = "2")
        private int messageUnreadCount;
        @Schema(description = "발신자 프로필 이미지 URL")
        private String profileImageUrl;
        @Schema(description = "첨부 파일 URL")
        private String fileUrl;
        @Schema(description = "첨부 파일명", example = "document.pdf")
        private String fileName;
        @Schema(description = "첨부 파일 유형", example = "application/pdf")
        private String fileType;
        @Schema(description = "첨부 파일 크기 (바이트)", example = "1048576")
        private Long fileSize;
        @Schema(description = "메시지 유형", example = "TEXT")
        private String messageType;

        public static ListRes from(ChatMessages entity, int messageUnreadCount, String profileImageUrl) {
            return ListRes.builder()
                    .idx(entity.getIdx())
                    .senderIdx(entity.getSender().getIdx())
                    .senderNickname(entity.getSender().getName())
                    .contents(entity.getContents())
                    .createdAt(entity.getCreatedAt())
                    .messageUnreadCount(messageUnreadCount)
                    .profileImageUrl(profileImageUrl)
                    .fileUrl(entity.getFileUrl())
                    .fileName(entity.getFileName())
                    .fileType(entity.getFileType())
                    .fileSize(entity.getFileSize())
                    .messageType(entity.getMessageType() != null ? entity.getMessageType().name() : "TEXT")
                    .build();
        }

        public static ListRes from(ChatMessages entity) {
            return from(entity, 0, null);
        }
    }
}
