package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatRoomsDto;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chatRoom")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final ParticipantsRepository participantsRepository;

    @PostMapping("/create")
    public ResponseEntity<Long> createRoom(
            @RequestBody ChatRoomsDto.ChatRoomsReq dto,
            @AuthenticationPrincipal AuthUserDetails user // 로그인한 내 정보
    ) {
        // 내 ID를 서비스에 함께 전달하여 참여자로 등록
        Long roomId = chatRoomService.createChatRoom(dto, user.getIdx());
        return ResponseEntity.status(HttpStatus.CREATED).body(roomId);
    }
    /**
     * 2. 기존 채팅방에 유저 추가 초대
     * POST /api/v1/chat/rooms/{roomId}/invite
     */
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteUsers(
            @PathVariable Long roomId,
            @RequestBody List<String> email // 초대할 유저 ID 리스트
    ) {
        chatRoomService.inviteUsersByEmail(roomId, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity list(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam(required = true, defaultValue = "0") int page,
            @RequestParam(required = true, defaultValue = "5") int size) {
        ChatRoomsDto.PageRes dto = chatRoomService.list(page, size, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success(dto));
    }
    // 나가기
    @DeleteMapping("/{roomIdx}/exit")
    public ResponseEntity exit(@PathVariable Long roomIdx,
                               @AuthenticationPrincipal AuthUserDetails user){
        chatRoomService.exit(roomIdx, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("성공"));
    }

    @PatchMapping("/{roomIdx}/title")
    public ResponseEntity updateTitle(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable Long roomIdx,
            @RequestBody ChatRoomsDto.UpdateTitleReq req) {

        chatRoomService.updateRoomTitle(roomIdx, req.getTitle(), user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("방 이름이 변경되었습니다."));
    }
    // 채팅방에 볼때
    @PostMapping("/{roomIdx}/enter")
    public ResponseEntity enter(
            @PathVariable Long roomIdx,
            @AuthenticationPrincipal AuthUserDetails user) {
        chatRoomService.enterRoom(roomIdx, user.getIdx());
        return ResponseEntity.ok().build();
    }

    // 채팅방 안보고있는 상태
    @PostMapping("/{roomIdx}/leave")
    public ResponseEntity leave(
            @PathVariable Long roomIdx,
            @AuthenticationPrincipal AuthUserDetails user) {
        chatRoomService.leaveRoom(roomIdx, user.getIdx());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomIdx}/heartbeat")
    public ResponseEntity heartbeat(
            @PathVariable Long roomIdx,
            @AuthenticationPrincipal AuthUserDetails user) {
        chatRoomService.refreshRoomPresence(roomIdx, user.getIdx());
        return ResponseEntity.ok().build();
    }
}
