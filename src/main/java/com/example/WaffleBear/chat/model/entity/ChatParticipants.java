package com.example.WaffleBear.chat.model.entity;

import com.example.WaffleBear.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChatParticipants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatRooms_idx")
    private ChatRooms chatRooms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_idx")
    private User users;

    // 각자가 설정한 방 이름
    @Setter
    private String customRoomName;

    // 알림 설정이나 즐겨찾기 관리
    private boolean isFavorite;

    /** 참여 시점: 이 시간 이후의 메시지만 노출 */
    private LocalDateTime joinedAt;

    /** 마지막으로 읽은 메시지 번호: 안 읽은 메시지 계산용 */
    private Long lastReadMessageId;

    public void updateLastReadMessageId(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    public void updateJoinedAt() {
        this.joinedAt = LocalDateTime.now();
    }
}

