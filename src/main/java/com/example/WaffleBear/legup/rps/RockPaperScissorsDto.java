package com.example.WaffleBear.legup.rps;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class RockPaperScissorsDto {

    @Schema(description = "방 참여 요청")
    public record JoinRequest(
            @Schema(description = "참여할 방 ID", example = "room-abc123")
            String roomId
    ) {}

    @Schema(description = "채팅 전송 요청")
    public record ChatRequest(
            @Schema(description = "방 ID", example = "room-abc123")
            String roomId,
            @Schema(description = "채팅 메시지", example = "안녕하세요!")
            String message
    ) {}

    @Schema(description = "타이핑 상태 전송 요청")
    public record TypingRequest(
            @Schema(description = "방 ID", example = "room-abc123")
            String roomId,
            @Schema(description = "타이핑 중 여부", example = "true")
            Boolean typing
    ) {}

    @Schema(description = "선택 제출 요청")
    public record ChoiceRequest(
            @Schema(description = "방 ID", example = "room-abc123")
            String roomId,
            @Schema(description = "선택 (ROCK/PAPER/SCISSORS)", example = "ROCK")
            String choice
    ) {}

    @Schema(description = "라운드 초기화 요청")
    public record ResetRequest(
            @Schema(description = "방 ID", example = "room-abc123")
            String roomId
    ) {}

    @Schema(description = "플레이어 정보")
    public record PlayerView(
            @Schema(description = "사용자 IDX", example = "1")
            Long userIdx,
            @Schema(description = "닉네임", example = "Player1")
            String nickname,
            @Schema(description = "이메일", example = "player@example.com")
            String email,
            @Schema(description = "연결 상태", example = "true")
            Boolean connected,
            @Schema(description = "선택 완료 여부", example = "false")
            Boolean choiceLocked,
            @Schema(description = "선택 (상대에게는 숨김)", example = "ROCK")
            String choice,
            @Schema(description = "승리 횟수", example = "2")
            Integer winCount
    ) {}

    @Schema(description = "채팅 메시지")
    public record MessageView(
            @Schema(description = "발신자 IDX", example = "1")
            Long userIdx,
            @Schema(description = "발신자 닉네임", example = "Player1")
            String nickname,
            @Schema(description = "메시지 내용", example = "안녕하세요!")
            String message,
            @Schema(description = "전송 시각")
            String createdAt
    ) {}

    @Schema(description = "라운드 결과")
    public record ResultView(
            @Schema(description = "결과 (WIN/LOSE/DRAW)", example = "WIN")
            String outcome,
            @Schema(description = "승자 IDX", example = "1")
            Long winnerUserIdx,
            @Schema(description = "승자 닉네임", example = "Player1")
            String winnerNickname,
            @Schema(description = "승자 선택", example = "ROCK")
            String winnerChoice,
            @Schema(description = "패자 IDX", example = "2")
            Long loserUserIdx,
            @Schema(description = "패자 닉네임", example = "Player2")
            String loserNickname,
            @Schema(description = "패자 선택", example = "SCISSORS")
            String loserChoice
    ) {}

    @Schema(description = "방 상태 (전체 스냅샷)")
    public record RoomState(
            @Schema(description = "방 ID", example = "room-abc123")
            String roomId,
            @Schema(description = "방 상태 (WAITING/PLAYING/RESULT)", example = "PLAYING")
            String status,
            @Schema(description = "플레이어 목록")
            List<PlayerView> players,
            @Schema(description = "채팅 메시지 목록")
            List<MessageView> messages,
            @Schema(description = "현재 타이핑 중인 사용자 닉네임")
            String typingNickname,
            @Schema(description = "라운드 결과")
            ResultView result,
            @Schema(description = "갱신 시각")
            String updatedAt
    ) {}

    @Schema(description = "퇴장 결과")
    public record LeaveOutcome(
            @Schema(description = "방 ID", example = "room-abc123")
            String roomId,
            @Schema(description = "퇴장 후 방 상태")
            RoomState state
    ) {}
}
