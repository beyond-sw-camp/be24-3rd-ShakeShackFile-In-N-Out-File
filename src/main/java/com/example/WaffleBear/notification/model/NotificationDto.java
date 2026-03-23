package com.example.WaffleBear.notification.model;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDto {

    public record Subscribe(
            Long userIdx,
            String endpoint,
            Map<String, String> keys
    ) {
        public NotificationEntity toEntity(Long userIdx) {
            return NotificationEntity.builder()
                    .userIdx(userIdx)
                    .endpoint(endpoint)
                    .p256dh(keys != null ? keys.get("p256dh") : null)
                    .auth(keys != null ? keys.get("auth") : null)
                    .build();
        }
    }

    public record Send(
            Long idx,
            String title,
            String message
    ) {
    }

    public record InboxItem(
            Long idx,
            String uuid,
            Long referenceId,
            String type,
            String title,
            String message,
            boolean read,
            LocalDateTime createdAt
    ) {
        public static InboxItem from(NotificationListEntity entity) {
            return new InboxItem(
                    entity.getIdx(),
                    entity.getUuid(),
                    entity.getReferenceId(),
                    entity.getType(),
                    entity.getTitle(),
                    entity.getMessage(),
                    entity.isRead(),
                    entity.getCreatedAt()
            );
        }
    }

    public record Target(
            Long id,
            String uuid
    ) {
    }

    public record Payload(
            Long notificationId,
            String type,
            String uuid,
            Long referenceId,
            String title,
            String message,
            Long roomIdx,
            Long unreadCount,
            String createdAt
    ) {
        public static Payload from(Send dto) {
            return new Payload(
                    null,
                    "general",
                    null,
                    null,
                    dto.title(),
                    dto.message(),
                    null,
                    0L,
                    null
            );
        }

        public static Payload create(String title, String message, Long roomIdx, Long unreadCount) {
            return new Payload(
                    null,
                    "message",
                    null,
                    null,
                    title,
                    message,
                    roomIdx,
                    unreadCount,
                    null
            );
        }

        public static Payload fromInbox(NotificationListEntity inbox) {
            return new Payload(
                    inbox.getIdx(),
                    inbox.getType(),
                    inbox.getUuid(),
                    inbox.getReferenceId(),
                    inbox.getTitle(),
                    inbox.getMessage(),
                    null,
                    0L,
                    inbox.getCreatedAt() != null ? inbox.getCreatedAt().toString() : null
            );
        }

        @Override
        public String toString() {
            return String.format(
                    "{\"notificationId\":%s,\"type\":\"%s\",\"uuid\":\"%s\",\"referenceId\":%s,\"title\":\"%s\",\"message\":\"%s\",\"roomIdx\":%s,\"unreadCount\":%d,\"createdAt\":\"%s\"}",
                    notificationId != null ? notificationId : "null",
                    type != null ? type : "general",
                    uuid != null ? uuid : "",
                    referenceId != null ? referenceId : "null",
                    escape(title),
                    escape(message),
                    roomIdx != null ? roomIdx : "null",
                    unreadCount != null ? unreadCount : 0L,
                    createdAt != null ? createdAt : ""
            );
        }

        private String escape(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
