package com.example.WaffleBear.chat.model.entity;

import com.example.WaffleBear.user.model.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChatParticipants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRooms_idx")
    private ChatRooms chatRooms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_idx")
    private Users users;

    // 각자가 설정한 방 이름
    private String customRoomName;

    // 알림 설정이나 즐겨찾기 관리
    private boolean isFavorite;
}

