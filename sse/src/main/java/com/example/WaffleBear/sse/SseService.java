package com.example.WaffleBear.sse;

import com.example.WaffleBear.sse.kafka.SseKafkaEvent;
import com.example.WaffleBear.sse.kafka.SseKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SseService {
    private final SseEmitterStore emitterStore;
    private final SseKafkaPublisher kafkaPublisher;

    public void sendTitleUpdate(Long postId, String newTitle, List<Long> userIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("title", newTitle);

        for (Long userId : userIds) {
            sendEventToUser(userId, "title-updated", data);
        }
    }

    public void sendRoleChanged(Long targetUserIdx, Long postIdx, String newRole) {
        Map<String, Object> payload = Map.of(
                "postIdx", postIdx,
                "newRole", newRole
        );
        sendEventToUser(targetUserIdx, "role-changed", payload);
    }

    public void sendEventToUser(Long userId, String eventName, Object data) {
        sendLocalEventToUser(userId, eventName, data);
        kafkaPublisher.publish(userId, eventName, data);
    }

    public void sendEventFromKafka(SseKafkaEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        sendLocalEventToUser(event.userId(), event.eventName(), event.payload());
    }

    public boolean isConnected(Long userId) {
        return emitterStore.getEmitters().containsKey(userId);
    }

    private void sendLocalEventToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitterStore.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            emitterStore.remove(userId);
            emitter.completeWithError(exception);
        }
    }
}
