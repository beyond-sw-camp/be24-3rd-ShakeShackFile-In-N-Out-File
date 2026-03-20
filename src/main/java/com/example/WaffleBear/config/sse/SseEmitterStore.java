package com.example.WaffleBear.config.sse;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SseEmitterStore {
    // 사용자별 active SSE emitter를 저장합니다.
    // controller에서 put하고 service에서 get/send 합니다.
    @Getter
    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void put(Long userId, SseEmitter emitter) {
        if (userId == null || emitter == null) return;
        emitters.put(userId, emitter);
    }

    public SseEmitter get(Long userId) {
        if (userId == null) return null;
        return emitters.get(userId);
    }

    public void remove(Long userId) {
        if (userId == null) return;
        emitters.remove(userId);
    }
}

