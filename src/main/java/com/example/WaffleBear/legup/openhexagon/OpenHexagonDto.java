package com.example.WaffleBear.legup.openhexagon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OpenHexagonDto {

    @Getter
    @Setter
    public static class JoinRequest {
        private String nickname;
    }

    @Getter
    @Setter
    public static class ReadyRequest {
        private Boolean ready;
    }

    @Getter
    @Setter
    public static class StateRequest {
        private Double angle;
        private Double scoreSeconds;
        private Boolean alive;
    }

    @Getter
    @Setter
    public static class ScoreRequest {
        private Double scoreSeconds;
    }

    @Getter
    @Builder
    public static class PatternFrame {
        private Integer sequence;
        private Integer spawnAtMs;
        private String patternType;
        private Integer safeStart;
        private Integer ringCount;
    }

    @Getter
    @Builder
    public static class MatchState {
        private String status;
        private Integer playerCount;
        private Integer readyCount;
        private Integer roundNumber;
        private Long roundSeed;
        private String countdownStartedAt;
        private String countdownEndsAt;
        private String roundStartsAt;
        private String serverTime;
        private List<PatternFrame> patterns;
    }

    @Getter
    @Builder
    public static class Presence {
        private Long userIdx;
        private String nickname;
        private String email;
        private Integer avatarIndex;
        private String accentColor;
        private Double angle;
        private Boolean ready;
        private Boolean alive;
        private Boolean playing;
        private Double currentScore;
        private Integer orbitSlot;
        private String joinedAt;
        private String lastSeenAt;
    }

    @Getter
    @Builder
    public static class LeaderboardEntry {
        private Long userIdx;
        private String nickname;
        private String email;
        private Integer avatarIndex;
        private String accentColor;
        private Double bestScore;
        private String updatedAt;
    }

    @Getter
    @Builder
    public static class LobbySnapshot {
        private Integer onlineCount;
        private List<Presence> players;
        private List<LeaderboardEntry> leaderboard;
        private MatchState match;
        private String updatedAt;
    }
}
