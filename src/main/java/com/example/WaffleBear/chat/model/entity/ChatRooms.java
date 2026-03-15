package com.example.WaffleBear.chat.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRooms {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;
    private LocalDateTime createdAt;
    @Setter
    private String title;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    @OneToMany(mappedBy = "chatRooms", fetch = FetchType.LAZY)
    private List<ChatParticipants> participants;

    public void updateLastMessage(String contents, LocalDateTime time) {
        this.lastMessage = contents;
        this.lastMessageTime = time;
    }
}
