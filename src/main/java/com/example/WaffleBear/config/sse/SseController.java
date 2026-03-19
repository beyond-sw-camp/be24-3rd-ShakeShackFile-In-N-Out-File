package com.example.WaffleBear.config.sse;

import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class SseController {
    private final Map<Long, SseEmitter> emitter_list;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @AuthenticationPrincipal AuthUserDetails user) {

        Long userIdx = user.getIdx();

        SseEmitter emitter = new SseEmitter(60 * 1000L * 60);
        emitter_list.put(userIdx, emitter);

        // 연결 종료나 타임아웃 발생 시 Map에서 제거 (중요: 메모리 누수 방지)
        emitter.onCompletion(() -> emitter_list.remove(userIdx));
        emitter.onTimeout(() -> emitter_list.remove(userIdx));
        emitter.onError((e) -> emitter_list.remove(userIdx));

        try {
            emitter.send(SseEmitter.event().name(
                    user.getName()+" => SSE").data("연결 성공"));
        }catch (IOException e) {
            emitter_list.remove(userIdx);
        }
        return emitter;
    }
//    @PostMapping(value = "/update")
//    public void update()
}
