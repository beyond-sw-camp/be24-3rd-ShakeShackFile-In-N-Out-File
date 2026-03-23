package com.example.WaffleBear.sse.kafka;

import com.example.WaffleBear.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wafflebear.sse.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SseKafkaConsumer {
    private final SseService sseService;
    private final SseKafkaProperties properties;

    @KafkaListener(
            topics = "#{@sseKafkaProperties.topic}",
            groupId = "#{@sseKafkaProperties.consumerGroupId}"
    )
    public void consume(SseKafkaEvent event) {
        if (event == null) {
            return;
        }

        if (properties.getInstanceId().equals(event.originInstanceId())) {
            return;
        }

        sseService.sendEventFromKafka(event);
    }
}
