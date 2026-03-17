package com.example.WaffleBear.notification.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDto {

    // ── 푸시 구독 ─────────────────────────────────────────────────────────────
    @Getter
    public static class Subscribe {
        private Long userIdx;
        private String endpoint;
        private Map<String, String> keys;

        public NotificationEntity toEntity(Long userIdx) {
            return NotificationEntity.builder()
                    .userIdx(userIdx)
                    .endpoint(this.endpoint)
                    .p256dh(this.keys.get("p256dh"))
                    .auth(this.keys.get("auth"))
                    .build();
        }
    }

    // ── 단건 전송 (관리자 등 직접 호출용) ─────────────────────────────────────
    @Getter
    public static class Send {
        private Long idx;
        private String title;
        private String message;
    }

    // ── 인박스 알림 응답 DTO (GET /notification/list 응답용) ──────────────────
    @Getter
    @Builder
    public static class InboxItem {
        private Long idx;
        private String uuid;
        private String type;
        private String title;
        private String message;
        private boolean read;
        private LocalDateTime createdAt;

        /** InboxNotificationEntity → InboxItem 변환 */
        public static InboxItem from(NotificationListEntity entity) {
            return InboxItem.builder()
                    .idx(entity.getIdx())
                    .uuid(entity.getUuid())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .message(entity.getMessage())
                    .read(entity.isRead())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    // ── 푸시 페이로드 ─────────────────────────────────────────────────────────
    @Getter
    @Builder
    public static class Payload {
        /** 알림 종류: "invite" | "message" | "general"  (sw.js 분기용) */
        private String type;
        /** 초대 수락/거절에 쓰이는 uuid */
        private String uuid;
        private String title;
        private String message;
        private Long roomIdx;
        private Long unreadCount;

        // 기존 단건 전송용
        public static Payload from(Send dto) {
            return Payload.builder()
                    .type("general")
                    .title(dto.getTitle())
                    .message(dto.getMessage())
                    .build();
        }

        // 기존 채팅 메시지 알림용
        public static Payload create(String title, String message, Long roomIdx, Long unreadCount) {
            return Payload.builder()
                    .type("message")
                    .title(title)
                    .message(message)
                    .roomIdx(roomIdx)
                    .unreadCount(unreadCount)
                    .build();
        }

        // ★ 추가: 워크스페이스 초대 알림용
        public static Payload createInvite(String title, String message, String uuid) {
            return Payload.builder()
                    .type("invite")
                    .uuid(uuid)
                    .title(title)
                    .message(message)
                    .roomIdx(null)
                    .unreadCount(0L)
                    .build();
        }

        @Override
        public String toString() {
            return String.format(
                    "{\"type\":\"%s\", \"uuid\":\"%s\", \"title\":\"%s\", \"message\":\"%s\", \"roomIdx\":%s, \"unreadCount\":%d}",
                    this.type      != null ? this.type    : "general",
                    this.uuid      != null ? this.uuid    : "",
                    this.title     != null ? this.title   : "",
                    this.message   != null ? this.message : "",
                    this.roomIdx   != null ? this.roomIdx : "null",
                    this.unreadCount != null ? this.unreadCount : 0L
            );
        }
    }
}