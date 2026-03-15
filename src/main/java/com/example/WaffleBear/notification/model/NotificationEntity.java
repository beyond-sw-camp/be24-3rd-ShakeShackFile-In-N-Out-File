package com.example.WaffleBear.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "notification")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;
    
    private Long userIdx;
    
    @Column(columnDefinition = "TEXT") // 엔드포인트는 매우 길 수 있으므로 TEXT 타입 권장
    private String endpoint;
    
    private String p256dh;
    private String auth;
}
