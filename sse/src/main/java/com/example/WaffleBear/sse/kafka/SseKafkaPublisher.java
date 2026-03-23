package com.example.WaffleBear.sse.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SseKafkaPublisher {
    private final Optional<KafkaTemplate<Object, Object>> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SseKafkaProperties properties;

    public void publish(Long userId, String eventName, Object data) {
        if (!properties.isEnabled() || userId == null || eventName == null || eventName.isBlank()) {
            return;
        }

        kafkaTemplate.ifPresent(template -> template.send(
                properties.getTopic(),
                String.valueOf(userId),
                SseKafkaEvent.builder()
                        .userId(userId)
                        .eventName(eventName)
                        .payload(objectMapper.valueToTree(data))
                        .originInstanceId(properties.getInstanceId())
                        .publishedAt(Instant.now())
                        .build()
        ));
    }
}
