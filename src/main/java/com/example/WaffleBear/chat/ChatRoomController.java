package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatRoomsDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;

@Tag(name = "채팅방 (ChatRoom)", description = "채팅방 생성, 초대, 목록 조회, 나가기 등 채팅방 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chatRoom")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final ParticipantsRepository participantsRepository;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성하고 참여자를 초대합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/create")
    public ResponseEntity<Long> createRoom(
            @RequestBody ChatRoomsDto.ChatRoomsReq dto,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user // 로그인한 내 정보
    ) {
        // 내 ID를 서비스에 함께 전달하여 참여자로 등록
        Long roomId = chatRoomService.createChatRoom(dto, user.getIdx());
        return ResponseEntity.status(HttpStatus.CREATED).body(roomId);
    }
    /**
     * 2. 기존 채팅방에 유저 추가 초대
     * POST /api/v1/chat/rooms/{roomId}/invite
     */
    @Operation(summary = "채팅방 유저 초대", description = "기존 채팅방에 이메일 목록으로 유저를 초대합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteUsers(
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomId,
            @RequestBody List<String> email // 초대할 유저 ID 리스트
    ) {
        chatRoomService.inviteUsersByEmail(roomId, email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅방 목록 조회", description = "로그인한 사용자가 참여 중인 채팅방 목록을 페이징하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/list")
    public ResponseEntity list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(required = true, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "5") @RequestParam(required = true, defaultValue = "5") int size) {
        ChatRoomsDto.PageRes dto = chatRoomService.list(page, size, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success(dto));
    }
    @Operation(summary = "채팅방 나가기", description = "채팅방에서 완전히 퇴장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "나가기 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    // 나가기
    @DeleteMapping("/{roomIdx}/exit")
    public ResponseEntity exit(@Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
                               @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user){
        chatRoomService.exit(roomIdx, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("성공"));
    }

    @Operation(summary = "채팅방 이름 변경", description = "채팅방의 제목을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이름 변경 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/{roomIdx}/title")
    public ResponseEntity updateTitle(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
            @RequestBody ChatRoomsDto.UpdateTitleReq req) {

        chatRoomService.updateRoomTitle(roomIdx, req.getTitle(), user.getIdx());
        return ResponseEntity.ok(BaseResponse.success("방 이름이 변경되었습니다."));
    }
    @Operation(summary = "채팅방 입장", description = "채팅방을 보고 있는 상태로 전환합니다. (읽음 처리용)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입장 성공")
    })
    // 채팅방에 볼때
    @PostMapping("/{roomIdx}/enter")
    public ResponseEntity enter(
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user) {
        chatRoomService.enterRoom(roomIdx, user.getIdx());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅방 퇴장 (보기 중단)", description = "채팅방을 더 이상 보고 있지 않은 상태로 전환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "퇴장 성공")
    })
    // 채팅방 안보고있는 상태
    @PostMapping("/{roomIdx}/leave")
    public ResponseEntity leave(
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long roomIdx,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user) {
        chatRoomService.leaveRoom(roomIdx, user.getIdx());
        return ResponseEntity.ok().build();
    }
    //수정
}
