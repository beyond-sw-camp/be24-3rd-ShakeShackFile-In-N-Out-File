package com.example.WaffleBear.chat.model.entity;


import com.example.WaffleBear.user.model.User;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Entity
@Builder
@NoArgsConstructor
public class ChatMessages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rooms_idx")
    private ChatRooms chatRooms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_idx")
    private User sender;

    private String contents;
    private LocalDateTime sendTime;

}
