package com.example.WaffleBear.sse;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SseEmitterStore {
    @Getter
    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void put(Long userId, SseEmitter emitter) {
        if (userId == null || emitter == null) {
            return;
        }
        emitters.put(userId, emitter);
    }

    public SseEmitter get(Long userId) {
        if (userId == null) {
            return null;
        }
        return emitters.get(userId);
    }

    public void remove(Long userId) {
        if (userId == null) {
            return;
        }
        emitters.remove(userId);
    }
}
