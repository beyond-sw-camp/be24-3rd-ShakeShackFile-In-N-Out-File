package com.example.WaffleBear.legup.openhexagon;

import com.example.WaffleBear.legup.LegupGameAccessService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * OpenHexagon WebSocket 컨트롤러.
 * STOMP 목적지 접두사: /pub/game/openhexagon/*
 * 구독 경로: /sub/game/openhexagon/lobby
 */
@Tag(name = "미니게임 - OpenHexagon", description = "OpenHexagon 미니게임 WebSocket STOMP 컨트롤러 (실시간 멀티플레이)")
@Controller
@RequiredArgsConstructor
public class OpenHexagonSocketController {

    private final LegupGameAccessService legupGameAccessService;
    private final OpenHexagonService openHexagonService;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "게임 참여 (WebSocket)", description = "OpenHexagon 게임 로비에 참여합니다. 목적지: /pub/game/openhexagon/join")
    @MessageMapping("/game/openhexagon/join")
    public void join(
            @Payload OpenHexagonDto.JoinRequest req,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(openHexagonService.join(headerAccessor.getSessionId(), user, req));
    }

    @Operation(summary = "준비 상태 변경 (WebSocket)", description = "게임 시작 전 준비 상태를 변경합니다. 목적지: /pub/game/openhexagon/ready")
    @MessageMapping("/game/openhexagon/ready")
    public void ready(
            @Payload OpenHexagonDto.ReadyRequest req,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(openHexagonService.setReady(headerAccessor.getSessionId(), user, req));
    }

    @Operation(summary = "점수 제출 (WebSocket)", description = "게임 종료 후 최종 점수를 제출합니다. 목적지: /pub/game/openhexagon/score")
    @MessageMapping("/game/openhexagon/score")
    public void score(
            @Payload OpenHexagonDto.ScoreRequest req,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(openHexagonService.submitScore(headerAccessor.getSessionId(), user, req));
    }

    @Operation(summary = "게임 상태 업데이트 (WebSocket)", description = "플레이 중 각도/점수/생존 상태를 실시간 업데이트합니다. 목적지: /pub/game/openhexagon/state")
    @MessageMapping("/game/openhexagon/state")
    public void state(
            @Payload OpenHexagonDto.StateRequest req,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(openHexagonService.updateState(headerAccessor.getSessionId(), user, req));
    }

    @Operation(summary = "게임 퇴장 (WebSocket)", description = "OpenHexagon 게임 로비에서 퇴장합니다. 목적지: /pub/game/openhexagon/leave")
    @MessageMapping("/game/openhexagon/leave")
    public void leave(
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        requireUser(principal);
        broadcast(openHexagonService.leave(headerAccessor.getSessionId()));
    }

    private void broadcast(OpenHexagonDto.LobbySnapshot snapshot) {
        messagingTemplate.convertAndSend("/sub/game/openhexagon/lobby", snapshot);
    }

    private AuthUserDetails requireUser(Principal principal) {
        Authentication auth = (Authentication) principal;
        return (AuthUserDetails) auth.getPrincipal();
    }
}
