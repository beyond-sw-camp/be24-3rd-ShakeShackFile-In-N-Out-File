package com.example.WaffleBear.notification.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

public class NotificationDto {
    @Getter
    public static class Subscribe {
        private Long userIdx; // 추가
        private String endpoint;
        private Map<String, String> keys;

        public NotificationEntity toEntity(Long userIdx) {
            return NotificationEntity.builder()
                    .userIdx(userIdx) // 추가
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
    public static class Payload {
        private String title;
        private String message;
        private Long roomIdx;

        public static Payload from(Send dto) {
            return Payload.builder()
                    .title(dto.getTitle())
                    .message(dto.message)
                    .build();
        }

        // 추가: 메시지 직접 생성용
        public static Payload create(String title, String message, Long roomIdx) {
            return Payload.builder()
                    .title(title)
                    .message(message)
                    .roomIdx(roomIdx)
                    .build();
        }

        @Override
        public String toString() {
            return String.format(
                    "{\"title\":\"%s\", \"message\":\"%s\", \"roomIdx\":%d}",
                    this.title,
                    this.message,
                    this.roomIdx // <-- 이 부분을 추가하세요!
            );
        }
    }
}
