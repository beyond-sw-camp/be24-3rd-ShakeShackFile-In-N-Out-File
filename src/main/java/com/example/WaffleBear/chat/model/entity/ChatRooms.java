package com.example.WaffleBear.chat.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String title;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    @OneToMany(mappedBy = "chatRooms", fetch = FetchType.LAZY)
    private List<ChatParticipants> participants;
}
