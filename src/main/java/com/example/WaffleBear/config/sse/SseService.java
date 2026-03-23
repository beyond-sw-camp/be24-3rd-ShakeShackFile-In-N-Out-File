package com.example.WaffleBear.config.sse;

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

    public void sendTitleUpdate(Long postId, String newTitle, List<Long> userIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("title", newTitle);

        for (Long userId : userIds) {
            sendEventToUser(userId, "title-updated", data);  // ✅ 통일
        }
    }

    public void sendRoleChanged(Long targetUserIdx, Long postIdx, String newRole) {
        Map<String, Object> payload = Map.of(
                "postIdx", postIdx,
                "newRole", newRole
        );
        sendEventToUser(targetUserIdx, "role-changed", payload);
    }
    // ✅ 공통 전송 헬퍼
    public void sendEventToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitterStore.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            emitterStore.remove(userId);
            emitter.completeWithError(e);
        }
    }
    public boolean isConnected(Long userId) {
        return emitterStore.getEmitters().containsKey(userId);
    }
}
