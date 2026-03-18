package com.example.WaffleBear.notification.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDto {

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

    @Getter
    public static class Send {
        private Long idx;
        private String title;
        private String message;
    }

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

    @Getter
    public static class Target {
        private Long id;
        private String uuid;
    }

    @Getter
    @Builder
    public static class Payload {
        private Long notificationId;
        private String type;
        private String uuid;
        private String title;
        private String message;
        private Long roomIdx;
        private Long unreadCount;
        private String createdAt;

        public static Payload from(Send dto) {
            return Payload.builder()
                    .type("general")
                    .title(dto.getTitle())
                    .message(dto.getMessage())
                    .build();
        }

        public static Payload create(String title, String message, Long roomIdx, Long unreadCount) {
            return Payload.builder()
                    .type("message")
                    .title(title)
                    .message(message)
                    .roomIdx(roomIdx)
                    .unreadCount(unreadCount)
                    .build();
        }

        public static Payload createInvite(NotificationListEntity inbox) {
            return Payload.builder()
                    .notificationId(inbox.getIdx())
                    .type("invite")
                    .uuid(inbox.getUuid())
                    .title(inbox.getTitle())
                    .message(inbox.getMessage())
                    .roomIdx(null)
                    .unreadCount(0L)
                    .createdAt(inbox.getCreatedAt() != null ? inbox.getCreatedAt().toString() : null)
                    .build();
        }

        @Override
        public String toString() {
            return String.format(
                    "{\"notificationId\":%s,\"type\":\"%s\",\"uuid\":\"%s\",\"title\":\"%s\",\"message\":\"%s\",\"roomIdx\":%s,\"unreadCount\":%d,\"createdAt\":\"%s\"}",
                    this.notificationId != null ? this.notificationId : "null",
                    this.type != null ? this.type : "general",
                    this.uuid != null ? this.uuid : "",
                    escape(this.title),
                    escape(this.message),
                    this.roomIdx != null ? this.roomIdx : "null",
                    this.unreadCount != null ? this.unreadCount : 0L,
                    this.createdAt != null ? this.createdAt : ""
            );
        }

        private String escape(String value) {
            if (value == null) return "";
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
