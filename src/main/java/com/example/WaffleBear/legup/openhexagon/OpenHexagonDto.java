package com.example.WaffleBear.legup.openhexagon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OpenHexagonDto {

    @Schema(description = "게임 참여 요청")
    @Getter
    @Setter
    public static class JoinRequest {
        @Schema(description = "플레이어 닉네임", example = "Player1")
        private String nickname;
    }

    @Schema(description = "준비 상태 변경 요청")
    @Getter
    @Setter
    public static class ReadyRequest {
        @Schema(description = "준비 상태 (true: 준비 완료)", example = "true")
        private Boolean ready;
    }

    @Schema(description = "게임 상태 업데이트 요청")
    @Getter
    @Setter
    public static class StateRequest {
        @Schema(description = "현재 각도", example = "45.5")
        private Double angle;
        @Schema(description = "생존 시간 (초)", example = "30.5")
        private Double scoreSeconds;
        @Schema(description = "생존 여부", example = "true")
        private Boolean alive;
    }

    @Schema(description = "점수 제출 요청")
    @Getter
    @Setter
    public static class ScoreRequest {
        @Schema(description = "최종 생존 시간 (초)", example = "45.2")
        private Double scoreSeconds;
    }

    @Schema(description = "패턴 프레임 정보")
    @Getter
    @Builder
    public static class PatternFrame {
        @Schema(description = "시퀀스 번호", example = "1")
        private Integer sequence;
        @Schema(description = "스폰 시간 (ms)", example = "5000")
        private Integer spawnAtMs;
        @Schema(description = "패턴 유형", example = "SPIRAL")
        private String patternType;
        @Schema(description = "안전 시작 위치", example = "0")
        private Integer safeStart;
        @Schema(description = "링 개수", example = "6")
        private Integer ringCount;
    }

    @Schema(description = "매치 상태 정보")
    @Getter
    @Builder
    public static class MatchState {
        @Schema(description = "매치 상태 (WAITING/COUNTDOWN/PLAYING/FINISHED)", example = "PLAYING")
        private String status;
        @Schema(description = "참여 플레이어 수", example = "4")
        private Integer playerCount;
        @Schema(description = "준비 완료 플레이어 수", example = "4")
        private Integer readyCount;
        @Schema(description = "라운드 번호", example = "1")
        private Integer roundNumber;
        @Schema(description = "라운드 시드 값", example = "123456789")
        private Long roundSeed;
        @Schema(description = "카운트다운 시작 시각")
        private String countdownStartedAt;
        @Schema(description = "카운트다운 종료 시각")
        private String countdownEndsAt;
        @Schema(description = "라운드 시작 시각")
        private String roundStartsAt;
        @Schema(description = "서버 현재 시각")
        private String serverTime;
        @Schema(description = "패턴 프레임 목록")
        private List<PatternFrame> patterns;
    }

    @Schema(description = "플레이어 상태 정보")
    @Getter
    @Builder
    public static class Presence {
        @Schema(description = "사용자 IDX", example = "1")
        private Long userIdx;
        @Schema(description = "닉네임", example = "Player1")
        private String nickname;
        @Schema(description = "이메일", example = "player@example.com")
        private String email;
        @Schema(description = "아바타 인덱스", example = "0")
        private Integer avatarIndex;
        @Schema(description = "강조 색상", example = "#FF6B6B")
        private String accentColor;
        @Schema(description = "현재 각도", example = "45.5")
        private Double angle;
        @Schema(description = "준비 상태", example = "true")
        private Boolean ready;
        @Schema(description = "생존 여부", example = "true")
        private Boolean alive;
        @Schema(description = "플레이 중 여부", example = "true")
        private Boolean playing;
        @Schema(description = "현재 점수", example = "30.5")
        private Double currentScore;
        @Schema(description = "궤도 슬롯 번호", example = "0")
        private Integer orbitSlot;
        @Schema(description = "참여 시각")
        private String joinedAt;
        @Schema(description = "마지막 활동 시각")
        private String lastSeenAt;
    }

    @Schema(description = "리더보드 항목")
    @Getter
    @Builder
    public static class LeaderboardEntry {
        @Schema(description = "사용자 IDX", example = "1")
        private Long userIdx;
        @Schema(description = "닉네임", example = "Player1")
        private String nickname;
        @Schema(description = "이메일", example = "player@example.com")
        private String email;
        @Schema(description = "아바타 인덱스", example = "0")
        private Integer avatarIndex;
        @Schema(description = "강조 색상", example = "#FF6B6B")
        private String accentColor;
        @Schema(description = "최고 점수 (초)", example = "120.5")
        private Double bestScore;
        @Schema(description = "갱신 시각")
        private String updatedAt;
    }

    @Schema(description = "로비 스냅샷 (전체 상태)")
    @Getter
    @Builder
    public static class LobbySnapshot {
        @Schema(description = "온라인 플레이어 수", example = "4")
        private Integer onlineCount;
        @Schema(description = "플레이어 목록")
        private List<Presence> players;
        @Schema(description = "리더보드")
        private List<LeaderboardEntry> leaderboard;
        @Schema(description = "매치 상태")
        private MatchState match;
        @Schema(description = "갱신 시각")
        private String updatedAt;
    }
}
