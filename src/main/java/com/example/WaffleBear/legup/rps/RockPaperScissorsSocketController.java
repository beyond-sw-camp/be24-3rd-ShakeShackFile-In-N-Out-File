package com.example.WaffleBear.legup.rps;

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
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * 가위바위보 WebSocket 컨트롤러.
 * STOMP 목적지 접두사: /pub/game/rps/*
 * 구독 경로: /sub/game/rps/room/{roomId}
 */
@Tag(name = "미니게임 - 가위바위보 (RPS)", description = "가위바위보 미니게임 WebSocket STOMP 컨트롤러 (실시간 대전)")
@Controller
@RequiredArgsConstructor
public class RockPaperScissorsSocketController {

    private final LegupGameAccessService legupGameAccessService;
    private final RockPaperScissorsService rockPaperScissorsService;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "방 참여 (WebSocket)", description = "가위바위보 게임 방에 참여합니다. 목적지: /pub/game/rps/join")
    @MessageMapping("/game/rps/join")
    public void join(
            @Payload RockPaperScissorsDto.JoinRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(rockPaperScissorsService.join(headerAccessor.getSessionId(), user, request));
    }

    @Operation(summary = "채팅 전송 (WebSocket)", description = "게임 방에서 채팅 메시지를 전송합니다. 목적지: /pub/game/rps/chat")
    @MessageMapping("/game/rps/chat")
    public void chat(
            @Payload RockPaperScissorsDto.ChatRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(rockPaperScissorsService.sendChat(headerAccessor.getSessionId(), user, request));
    }

    @Operation(summary = "타이핑 상태 전송 (WebSocket)", description = "채팅 입력 중 상태를 전송합니다. 목적지: /pub/game/rps/typing")
    @MessageMapping("/game/rps/typing")
    public void typing(
            @Payload RockPaperScissorsDto.TypingRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(rockPaperScissorsService.updateTyping(headerAccessor.getSessionId(), user, request));
    }

    @Operation(summary = "선택 제출 (WebSocket)", description = "가위/바위/보 중 하나를 선택하여 제출합니다. 목적지: /pub/game/rps/choice")
    @MessageMapping("/game/rps/choice")
    public void choice(
            @Payload RockPaperScissorsDto.ChoiceRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(rockPaperScissorsService.submitChoice(headerAccessor.getSessionId(), user, request));
    }

    @Operation(summary = "라운드 초기화 (WebSocket)", description = "게임 라운드를 초기화합니다. 목적지: /pub/game/rps/reset")
    @MessageMapping("/game/rps/reset")
    public void reset(
            @Payload RockPaperScissorsDto.ResetRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        AuthUserDetails user = requireUser(principal);
        legupGameAccessService.ensurePlayable(user);
        broadcast(rockPaperScissorsService.resetRound(headerAccessor.getSessionId(), user, request));
    }

    @Operation(summary = "방 퇴장 (WebSocket)", description = "가위바위보 게임 방에서 퇴장합니다. 목적지: /pub/game/rps/leave")
    @MessageMapping("/game/rps/leave")
    public void leave(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        requireUser(principal);
        broadcast(rockPaperScissorsService.leave(headerAccessor.getSessionId()));
    }

    public void broadcast(RockPaperScissorsDto.RoomState state) {
        if (state == null || !StringUtils.hasText(state.roomId())) {
            return;
        }

        messagingTemplate.convertAndSend("/sub/game/rps/room/" + state.roomId(), state);
    }

    public void broadcast(RockPaperScissorsDto.LeaveOutcome outcome) {
        if (outcome == null || outcome.state() == null || !StringUtils.hasText(outcome.roomId())) {
            return;
        }

        messagingTemplate.convertAndSend("/sub/game/rps/room/" + outcome.roomId(), outcome.state());
    }

    private AuthUserDetails requireUser(Principal principal) {
        Authentication authentication = (Authentication) principal;
        return (AuthUserDetails) authentication.getPrincipal();
    }
}
