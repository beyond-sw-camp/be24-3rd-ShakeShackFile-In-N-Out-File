package com.example.WaffleBear.sse.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SseKafkaEvent(
        Long userId,
        String eventName,
        JsonNode payload,
        String originInstanceId,
        Instant publishedAt
) {
}
