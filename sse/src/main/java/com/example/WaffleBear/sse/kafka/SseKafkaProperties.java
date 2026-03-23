package com.example.WaffleBear.sse.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "wafflebear.sse.kafka")
public class SseKafkaProperties {
    private boolean enabled = true;
    private String topic = "wafflebear.sse.events";
    private String groupIdPrefix = "wafflebear-sse";
    private String instanceId = UUID.randomUUID().toString();

    public String getConsumerGroupId() {
        return groupIdPrefix + "-" + instanceId;
    }
}
