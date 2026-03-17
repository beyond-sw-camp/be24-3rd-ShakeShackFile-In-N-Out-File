package com.example.WaffleBear.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "notification_list")
public class NotificationListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    /** 알림을 받을 유저 idx */
    private Long receiverUserIdx;

    /** 초대 수락/거절에 쓰이는 uuid (초대 알림이 아닐 경우 null) */
    private String uuid;

    /** 알림 종류: "invite" | "general" */
    private String type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "`read`")
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}