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
            SseEmitter emitter = emitterStore.get(userId);
            if (emitter != null) {
                try {
                    // 이벤트 이름을 프론트엔드 addEventListener와 일치시킴
                    emitter.send(SseEmitter.event()
                            .name("title-updated")
                            .data(data));
                } catch (IOException e) {
                    emitterStore.remove(userId);
                }
            }
        }
    }
}
