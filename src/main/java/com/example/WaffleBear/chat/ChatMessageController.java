package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatMessagesDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@Tag(name = "채팅 메시지 (ChatMessage)", description = "채팅 메시지 조회, 전송, 읽음 처리, 파일 업로드, 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatMessageController {
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1. 특정 채팅방의 메시지 목록 조회 (HTTP)
     */
    @Operation(summary = "채팅 메시지 목록 조회", description = "특정 채팅방의 메시지를 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @GetMapping("/{roomIdx}/history")
    public BaseResponse list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user, // 유저 확인이 필요한 경우 추가
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size) {
        ChatMessagesDto.PageRes dto = chatMessageService.getMessageList(roomIdx,user.getIdx(), page, size);
        return BaseResponse.success(dto);
    }

    /**
     * 2. 실시간 메시지 전송 (WebSocket)
     * @AuthenticationPrincipal은 웹소켓 연결 시 인증이 완료된 상태라면 사용 가능합니다.
     */
    @Operation(summary = "실시간 메시지 전송 (WebSocket)", description = "WebSocket STOMP를 통해 채팅 메시지를 실시간으로 전송합니다. 구독 경로: /sub/chat/room/{roomIdx}")
    @MessageMapping("/chat/{roomIdx}")
    public void sendMessage(
            @DestinationVariable Long roomIdx,
            @Payload ChatMessagesDto.Send req,
            Principal principal) {

        Authentication auth = (Authentication) principal;
        AuthUserDetails user = (AuthUserDetails) auth.getPrincipal();

        if (user == null || user.getIdx() == null) {
            // 인증 정보가 없으면 처리 중단
            return;
        }
        // user.getIdx()를 사용하여 실제 작성자 정보를 서비스에 전달
        ChatMessagesDto.ListRes savedMsg = chatMessageService.saveMessage(roomIdx, req, user.getIdx());

        // 해당 방 구독자들에게 실시간 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomIdx, savedMsg);
    }

    @Operation(summary = "메시지 읽음 처리", description = "특정 채팅방의 메시지를 모두 읽음으로 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 완료")
    })
    @PostMapping("/{roomIdx}/read")
    public ResponseEntity read(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx) {
        chatMessageService.markAsRead(roomIdx, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("읽음 처리 완료"));
    }

    @Operation(summary = "채팅 파일 업로드", description = "채팅방에 파일을 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식")
    })
    @PostMapping("/{roomIdx}/upload")
    public ResponseEntity upload(
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
            @Parameter(description = "업로드할 파일") @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user) {

        String fileUrl = chatMessageService.uploadFile(roomIdx, file, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success(Map.of("fileUrl", fileUrl)));
    }

    @Operation(summary = "채팅 메시지 삭제", description = "특정 채팅 메시지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 삭제 완료"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음")
    })
    @DeleteMapping("/{roomIdx}/{messageIdx}")
    public ResponseEntity<?> deleteMessage(
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
            @Parameter(description = "메시지 ID", example = "10") @PathVariable Long messageIdx,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user) {

        chatMessageService.deleteMessage(roomIdx, messageIdx, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("메시지 삭제 완료"));
    }
}