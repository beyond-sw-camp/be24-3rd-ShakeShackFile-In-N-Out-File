package com.example.WaffleBear.config.sse;

import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.IOException;

@Tag(name = "SSE (Server-Sent Events)", description = "서버에서 클라이언트로 실시간 이벤트를 전송하는 SSE 연결 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class SseController {
    private final SseEmitterStore emitterStore;

    @Operation(summary = "SSE 연결", description = "서버와 SSE 연결을 맺습니다. 연결 후 실시간 알림을 수신할 수 있습니다. 타임아웃: 60분")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user) {

        if (user == null) {
            // JwtFilter/인증 실패 케이스 방어 (SecurityConfig.anyRequest().authenticated로 원칙적으로 차단됨)
            return new SseEmitter(0L);
        }

        Long userIdx = user.getIdx();

        SseEmitter emitter = new SseEmitter(60 * 1000L * 60);
        emitterStore.put(userIdx, emitter);

        // 연결 종료나 타임아웃 발생 시 Map에서 제거 (중요: 메모리 누수 방지)
        emitter.onCompletion(() -> emitterStore.remove(userIdx));
        emitter.onTimeout(() -> emitterStore.remove(userIdx));
        emitter.onError((e) -> emitterStore.remove(userIdx));

        try {
            emitter.send(SseEmitter.event().name(
                    user.getName()+" => SSE").data("연결 성공"));
        }catch (IOException e) {
            emitterStore.remove(userIdx);
        }
        return emitter;
    }
}
