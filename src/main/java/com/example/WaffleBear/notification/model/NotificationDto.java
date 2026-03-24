package com.example.WaffleBear.notification.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDto {

    @Schema(description = "푸시 알림 구독 요청")
    public record Subscribe(
            @Schema(description = "사용자 IDX", example = "1")
            Long userIdx,
            @Schema(description = "브라우저 Push API 엔드포인트 URL", example = "https://fcm.googleapis.com/fcm/send/...")
            String endpoint,
            @Schema(description = "Push API 암호화 키 (p256dh, auth)")
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

    @Schema(description = "알림 발송 요청")
    public record Send(
            @Schema(description = "수신 대상 사용자 IDX", example = "1")
            Long idx,
            @Schema(description = "알림 제목", example = "새로운 파일이 공유되었습니다")
            String title,
            @Schema(description = "알림 내용", example = "홍길동님이 파일을 공유했습니다.")
            String message
    ) {
    }

    @Schema(description = "수신함 알림 항목")
    public record InboxItem(
            @Schema(description = "알림 고유 번호", example = "1")
            Long idx,
            @Schema(description = "알림 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid,
            @Schema(description = "참조 대상 ID", example = "10")
            Long referenceId,
            @Schema(description = "알림 유형", example = "FILE_SHARE")
            String type,
            @Schema(description = "알림 제목", example = "새로운 파일이 공유되었습니다")
            String title,
            @Schema(description = "알림 내용", example = "홍길동님이 파일을 공유했습니다.")
            String message,
            @Schema(description = "읽음 여부", example = "false")
            boolean read,
            @Schema(description = "알림 생성 일시", example = "2026-03-24T10:00:00")
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

    @Schema(description = "알림 대상 지정 (읽음 처리/삭제용)")
    public record Target(
            @Schema(description = "알림 ID", example = "1")
            Long id,
            @Schema(description = "알림 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid
    ) {
    }

    @Schema(description = "푸시 알림 페이로드")
    public record Payload(
            @Schema(description = "알림 ID", example = "1")
            Long notificationId,
            @Schema(description = "알림 유형", example = "general")
            String type,
            @Schema(description = "알림 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid,
            @Schema(description = "참조 대상 ID", example = "10")
            Long referenceId,
            @Schema(description = "알림 제목", example = "새 메시지")
            String title,
            @Schema(description = "알림 내용", example = "새로운 메시지가 도착했습니다.")
            String message,
            @Schema(description = "채팅방 IDX", example = "5")
            Long roomIdx,
            @Schema(description = "읽지 않은 알림 수", example = "3")
            Long unreadCount,
            @Schema(description = "생성 일시 문자열", example = "2026-03-24T10:00:00")
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
