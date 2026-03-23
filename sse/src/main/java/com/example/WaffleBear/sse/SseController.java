package com.example.WaffleBear.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class SseController {
    private static final long SSE_TIMEOUT_MILLIS = 60L * 60 * 1000;

    private final SseEmitterStore emitterStore;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @AuthenticationPrincipal(expression = "idx") Long userIdx,
            @AuthenticationPrincipal(expression = "name") String userName) {

        if (userIdx == null) {
            return new SseEmitter(0L);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitterStore.put(userIdx, emitter);

        emitter.onCompletion(() -> emitterStore.remove(userIdx));
        emitter.onTimeout(() -> emitterStore.remove(userIdx));
        emitter.onError(exception -> emitterStore.remove(userIdx));

        try {
            emitter.send(SseEmitter.event()
                    .name("sse-connected")
                    .data((userName != null ? userName : "anonymous") + " connected"));
        } catch (IOException exception) {
            emitterStore.remove(userIdx);
            emitter.completeWithError(exception);
        }

        return emitter;
    }
}
