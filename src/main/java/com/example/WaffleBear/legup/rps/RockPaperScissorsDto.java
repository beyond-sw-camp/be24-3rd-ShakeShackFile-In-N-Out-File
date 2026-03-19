package com.example.WaffleBear.legup.rps;

import java.util.List;

public class RockPaperScissorsDto {

    public record JoinRequest(String roomId) {}

    public record ChatRequest(String roomId, String message) {}

    public record TypingRequest(String roomId, Boolean typing) {}

    public record ChoiceRequest(String roomId, String choice) {}

    public record ResetRequest(String roomId) {}

    public record PlayerView(
            Long userIdx,
            String nickname,
            String email,
            Boolean connected,
            Boolean choiceLocked,
            String choice,
            Integer winCount
    ) {}

    public record MessageView(
            Long userIdx,
            String nickname,
            String message,
            String createdAt
    ) {}

    public record ResultView(
            String outcome,
            Long winnerUserIdx,
            String winnerNickname,
            String winnerChoice,
            Long loserUserIdx,
            String loserNickname,
            String loserChoice
    ) {}

    public record RoomState(
            String roomId,
            String status,
            List<PlayerView> players,
            List<MessageView> messages,
            String typingNickname,
            ResultView result,
            String updatedAt
    ) {}

    public record LeaveOutcome(String roomId, RoomState state) {}
}
