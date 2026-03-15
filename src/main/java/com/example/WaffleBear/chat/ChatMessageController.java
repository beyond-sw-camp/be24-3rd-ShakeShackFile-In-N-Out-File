package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatMessagesDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatMessageController {
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1. 특정 채팅방의 메시지 목록 조회 (HTTP)
     */
    @GetMapping("/{roomIdx}/history")
    public BaseResponse list(
            @AuthenticationPrincipal AuthUserDetails user, // 유저 확인이 필요한 경우 추가
            @PathVariable Long roomIdx,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        System.out.println("=== /chat/" + roomIdx + " 호출 ===");
        System.out.println("user: " + user);  // null이면 JWT 필터 문제
        ChatMessagesDto.PageRes dto = chatMessageService.getMessageList(roomIdx,user.getIdx(), page, size);
        return BaseResponse.success(dto);
    }

    /**
     * 2. 실시간 메시지 전송 (WebSocket)
     * @AuthenticationPrincipal은 웹소켓 연결 시 인증이 완료된 상태라면 사용 가능합니다.
     */
    @MessageMapping("/chat/{roomIdx}")
    public void sendMessage(
            @DestinationVariable Long roomIdx,
            @Payload ChatMessagesDto.Send req,
            Principal principal) {

        Authentication auth = (Authentication) principal;
        AuthUserDetails user = (AuthUserDetails) auth.getPrincipal();
        System.out.println("=== 메시지 저장 시도 ===");
        System.out.println("roomIdx: " + roomIdx);
        System.out.println("userIdx: " + user.getIdx());
        System.out.println("message: " + req.getContents());

        if (user == null || user.getIdx() == null) {
            // 인증 정보가 없으면 처리 중단
            return;
        }
        // user.getIdx()를 사용하여 실제 작성자 정보를 서비스에 전달
        ChatMessagesDto.ListRes savedMsg = chatMessageService.saveMessage(roomIdx, req, user.getIdx());

        // 해당 방 구독자들에게 실시간 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomIdx, savedMsg);
    }

    @PostMapping("/{roomIdx}/read")
    public ResponseEntity read(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long roomIdx) {
        chatMessageService.markAsRead(roomIdx, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("읽음 처리 완료"));
    }
}