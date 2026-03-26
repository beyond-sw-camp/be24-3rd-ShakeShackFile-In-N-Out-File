package com.example.WaffleBear.chat.model.entity;


import com.example.WaffleBear.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

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

    // 파일 관련 컬럼 추가
    @Column(columnDefinition = "TEXT")
    private String fileUrl;      // MinIO 저장 URL

    private String fileName;     // 원본 파일명
    private String fileType;     // 파일 타입 (image/png, application/pdf 등)
    private Long fileSize;       // 파일 크기 (bytes)

    @Enumerated(EnumType.STRING)
    private MessageType messageType; // TEXT, IMAGE, FILE

    @CreatedDate // 👈 생성 시 시간 자동 기록
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public void markDeleted() {
        this.deleted = true;
        this.contents = "삭제된 메시지입니다.";
        this.fileUrl = null;
        this.fileName = null;
        this.fileType = null;
        this.fileSize = null;
        this.messageType = MessageType.TEXT;
    }


}
