package com.example.WaffleBear.chat.model.dto;

import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;

public class ChatMessagesDto {
    public static class ChatMessageReq{
        private String contents;   // 채팅 내용

        public ChatMessages toEntity(Long userIdx, Long chatRoomIdx){
            return ChatMessages.builder()
                    .contents(this.contents)
                    .sender(User.builder().idx(userIdx).build())
                    .chatRooms(ChatRooms.builder().idx(chatRoomIdx).build())
                    .build();

        }
    }
}
