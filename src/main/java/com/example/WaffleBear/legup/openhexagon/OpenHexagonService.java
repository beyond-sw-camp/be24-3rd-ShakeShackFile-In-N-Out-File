package com.example.WaffleBear.legup.openhexagon;

import com.example.WaffleBear.user.model.AuthUserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OpenHexagonService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int AVATAR_COUNT = 13;
    private static final double TAU = Math.PI * 2;
    private static final double DEFAULT_ANGLE = -Math.PI / 2;
    private static final long COUNTDOWN_SECONDS = 3L;
    private static final int PATTERN_COUNT = 150;
    private static final List<String> DEFAULT_COLORS = List.of(
            "#59d6ff", "#ff9261", "#ff4f88", "#7dffb2", "#ffd166", "#8f9dff", "#7cffe6",
            "#ffa5d8", "#95ff73", "#f8a3ff", "#8ce9ff", "#ffb86c", "#b7ff7a"
    );

    private final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, ScoreRecord> leaderboard = new ConcurrentHashMap<>();
    private final MatchRuntime match = new MatchRuntime();

    public synchronized OpenHexagonDto.LobbySnapshot join(
            String sessionId,
            AuthUserDetails user,
            OpenHexagonDto.JoinRequest req
    ) {
        LocalDateTime now = LocalDateTime.now();
        refreshMatchPhase(now);

        PlayerSession session = sessions.computeIfAbsent(sessionId, key -> new PlayerSession());
        CosmeticProfile cosmetics = resolveCosmetics(user.getIdx());

        session.setSessionId(sessionId);
        session.setUserIdx(user.getIdx());
        session.setEmail(user.getEmail());
        session.setNickname(resolveNickname(user, req != null ? req.getNickname() : null));
        session.setAvatarIndex(cosmetics.avatarIndex());
        session.setAccentColor(cosmetics.accentColor());
        session.setAngle(DEFAULT_ANGLE);
        session.setReady(false);
        session.setAlive(true);
        session.setPlaying(false);
        session.setCurrentScore(0D);
        session.setLastSeenAt(now);

        if (session.getJoinedAt() == null) {
            session.setJoinedAt(now);
        }

        if (!"LOBBY".equals(match.getStatus())) {
            resetMatch(now);
        }

        return getSnapshot();
    }

    public synchronized OpenHexagonDto.LobbySnapshot setReady(
            String sessionId,
            AuthUserDetails user,
            OpenHexagonDto.ReadyRequest req
    ) {
        LocalDateTime now = LocalDateTime.now();
        refreshMatchPhase(now);

        PlayerSession session = requireSession(sessionId, user);
        boolean ready = req != null && Boolean.TRUE.equals(req.getReady());

        if ("RUNNING".equals(match.getStatus())) {
            return getSnapshot();
        }

        session.setReady(ready);
        session.setLastSeenAt(now);

        if ("COUNTDOWN".equals(match.getStatus()) && !ready) {
            resetMatch(now);
            return getSnapshot();
        }

        if ("LOBBY".equals(match.getStatus()) && areAllPlayersReady()) {
            startCountdown(now);
        }

        return getSnapshot();
    }

    public synchronized OpenHexagonDto.LobbySnapshot updateState(
            String sessionId,
            AuthUserDetails user,
            OpenHexagonDto.StateRequest req
    ) {
        LocalDateTime now = LocalDateTime.now();
        refreshMatchPhase(now);

        PlayerSession session = sessions.get(sessionId);

        if (session == null) {
            return getSnapshot();
        }

        session.setUserIdx(user.getIdx());
        session.setEmail(user.getEmail());
        session.setLastSeenAt(now);

        if (req != null) {
            if (req.getAngle() != null) {
                session.setAngle(normalizeAngle(req.getAngle()));
            }
            if (req.getScoreSeconds() != null) {
                session.setCurrentScore(normalizeScore(req.getScoreSeconds()));
            }
            if (req.getAlive() != null) {
                session.setAlive(req.getAlive());
                session.setPlaying(Boolean.TRUE.equals(req.getAlive()) && "RUNNING".equals(match.getStatus()));
            }
        }

        refreshMatchPhase(now);
        return getSnapshot();
    }

    public synchronized OpenHexagonDto.LobbySnapshot submitScore(
            String sessionId,
            AuthUserDetails user,
            OpenHexagonDto.ScoreRequest req
    ) {
        LocalDateTime now = LocalDateTime.now();
        refreshMatchPhase(now);

        PlayerSession session = sessions.get(sessionId);
        double normalizedScore = normalizeScore(req != null ? req.getScoreSeconds() : null);

        String nickname = resolveNickname(user, null);
        Integer avatarIndex = 0;
        String accentColor = DEFAULT_COLORS.get(0);

        if (session != null) {
            session.setCurrentScore(normalizedScore);
            session.setAlive(false);
            session.setPlaying(false);
            session.setReady(false);
            session.setLastSeenAt(now);

            nickname = session.getNickname();
            avatarIndex = session.getAvatarIndex();
            accentColor = session.getAccentColor();
        } else {
            CosmeticProfile cosmetics = resolveCosmetics(user.getIdx());
            avatarIndex = cosmetics.avatarIndex();
            accentColor = cosmetics.accentColor();
        }

        String finalNickname = nickname;
        Integer finalAvatarIndex = avatarIndex;
        String finalAccentColor = accentColor;

        leaderboard.compute(user.getIdx(), (userIdx, existing) -> {
            double bestScore = normalizedScore;

            if (existing != null) {
                bestScore = Math.max(existing.bestScore(), normalizedScore);
            }

            return new ScoreRecord(
                    userIdx,
                    finalNickname,
                    user.getEmail(),
                    finalAvatarIndex,
                    finalAccentColor,
                    bestScore,
                    now
            );
        });

        refreshMatchPhase(now);
        return getSnapshot();
    }

    public synchronized OpenHexagonDto.LobbySnapshot leave(String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        PlayerSession removed = sessions.remove(sessionId);

        if (removed == null) {
            refreshMatchPhase(now);
            return getSnapshot();
        }

        if (sessions.isEmpty()) {
            resetMatch(now);
        } else if (!"LOBBY".equals(match.getStatus())) {
            resetMatch(now);
        }

        refreshMatchPhase(now);
        return getSnapshot();
    }

    public synchronized OpenHexagonDto.LobbySnapshot getSnapshot() {
        refreshMatchPhase(LocalDateTime.now());

        List<PlayerSession> uniquePlayers = getUniquePlayers();

        List<OpenHexagonDto.Presence> players = new ArrayList<>();
        for (int index = 0; index < uniquePlayers.size(); index += 1) {
            PlayerSession session = uniquePlayers.get(index);
            players.add(OpenHexagonDto.Presence.builder()
                    .userIdx(session.getUserIdx())
                    .nickname(session.getNickname())
                    .email(session.getEmail())
                    .avatarIndex(session.getAvatarIndex())
                    .accentColor(session.getAccentColor())
                    .angle(session.getAngle())
                    .ready(Boolean.TRUE.equals(session.getReady()))
                    .alive(Boolean.TRUE.equals(session.getAlive()))
                    .playing(Boolean.TRUE.equals(session.getPlaying()))
                    .currentScore(session.getCurrentScore())
                    .orbitSlot(index)
                    .joinedAt(format(session.getJoinedAt()))
                    .lastSeenAt(format(session.getLastSeenAt()))
                    .build());
        }

        List<OpenHexagonDto.LeaderboardEntry> topEntries = leaderboard.values().stream()
                .sorted(Comparator
                        .comparingDouble(ScoreRecord::bestScore).reversed()
                        .thenComparing(ScoreRecord::updatedAt))
                .limit(10)
                .map(record -> OpenHexagonDto.LeaderboardEntry.builder()
                        .userIdx(record.userIdx())
                        .nickname(record.nickname())
                        .email(record.email())
                        .avatarIndex(record.avatarIndex())
                        .accentColor(record.accentColor())
                        .bestScore(record.bestScore())
                        .updatedAt(format(record.updatedAt()))
                        .build())
                .toList();

        OpenHexagonDto.MatchState matchState = OpenHexagonDto.MatchState.builder()
                .status(match.getStatus())
                .playerCount(uniquePlayers.size())
                .readyCount((int) uniquePlayers.stream().filter(PlayerSession::getReady).count())
                .roundNumber(match.getRoundNumber())
                .roundSeed(match.getRoundSeed())
                .countdownStartedAt(format(match.getCountdownStartedAt()))
                .countdownEndsAt(format(match.getCountdownEndsAt()))
                .roundStartsAt(format(match.getRoundStartsAt()))
                .serverTime(format(LocalDateTime.now()))
                .patterns(match.getPatterns())
                .build();

        return OpenHexagonDto.LobbySnapshot.builder()
                .onlineCount(players.size())
                .players(players)
                .leaderboard(topEntries)
                .match(matchState)
                .updatedAt(format(LocalDateTime.now()))
                .build();
    }

    private PlayerSession requireSession(String sessionId, AuthUserDetails user) {
        PlayerSession session = sessions.get(sessionId);

        if (session == null) {
            PlayerSession created = new PlayerSession();
            created.setSessionId(sessionId);
            created.setUserIdx(user.getIdx());
            created.setEmail(user.getEmail());
            created.setNickname(resolveNickname(user, null));
            CosmeticProfile cosmetics = resolveCosmetics(user.getIdx());
            created.setAvatarIndex(cosmetics.avatarIndex());
            created.setAccentColor(cosmetics.accentColor());
            created.setAngle(DEFAULT_ANGLE);
            created.setAlive(true);
            created.setPlaying(false);
            created.setReady(false);
            created.setCurrentScore(0D);
            created.setJoinedAt(LocalDateTime.now());
            created.setLastSeenAt(LocalDateTime.now());
            sessions.put(sessionId, created);
            return created;
        }

        return session;
    }

    private List<PlayerSession> getUniquePlayers() {
        return sessions.values().stream()
                .filter(session -> session.getUserIdx() != null)
                .collect(Collectors.toMap(
                        PlayerSession::getUserIdx,
                        session -> session,
                        this::chooseLatest
                ))
                .values()
                .stream()
                .sorted(Comparator
                        .comparing(PlayerSession::getPlaying).reversed()
                        .thenComparing(PlayerSession::getReady).reversed()
                        .thenComparing(PlayerSession::getJoinedAt))
                .toList();
    }

    private PlayerSession chooseLatest(PlayerSession left, PlayerSession right) {
        if (left.getLastSeenAt() == null) {
            return right;
        }
        if (right.getLastSeenAt() == null) {
            return left;
        }
        return left.getLastSeenAt().isAfter(right.getLastSeenAt()) ? left : right;
    }

    private boolean areAllPlayersReady() {
        List<PlayerSession> uniquePlayers = getUniquePlayers();
        return !uniquePlayers.isEmpty() && uniquePlayers.stream().allMatch(PlayerSession::getReady);
    }

    private void startCountdown(LocalDateTime now) {
        match.setStatus("COUNTDOWN");
        match.setCountdownStartedAt(now);
        match.setCountdownEndsAt(now.plusSeconds(COUNTDOWN_SECONDS));
        match.setRoundStartsAt(now.plusSeconds(COUNTDOWN_SECONDS));
        match.setRoundNumber(match.getRoundNumber() + 1);
        match.setRoundSeed(Math.abs(new Random().nextLong()));
        match.setPatterns(generatePatterns(match.getRoundSeed()));

        for (PlayerSession session : sessions.values()) {
            session.setPlaying(false);
            session.setAlive(true);
            session.setAngle(DEFAULT_ANGLE);
            session.setCurrentScore(0D);
        }
    }

    private void refreshMatchPhase(LocalDateTime now) {
        if ("COUNTDOWN".equals(match.getStatus()) && match.getRoundStartsAt() != null
                && !now.isBefore(match.getRoundStartsAt())) {
            match.setStatus("RUNNING");
            for (PlayerSession session : sessions.values()) {
                session.setPlaying(Boolean.TRUE.equals(session.getReady()));
                session.setAlive(true);
                session.setAngle(DEFAULT_ANGLE);
                session.setCurrentScore(0D);
            }
        }

        if ("RUNNING".equals(match.getStatus())) {
            boolean anyPlaying = sessions.values().stream()
                    .anyMatch(session -> Boolean.TRUE.equals(session.getPlaying()) && Boolean.TRUE.equals(session.getAlive()));
            if (!anyPlaying) {
                resetMatch(now);
            }
        }
    }

    private void resetMatch(LocalDateTime now) {
        match.setStatus("LOBBY");
        match.setCountdownStartedAt(null);
        match.setCountdownEndsAt(null);
        match.setRoundStartsAt(null);
        match.setRoundSeed(null);
        match.setPatterns(List.of());

        for (PlayerSession session : sessions.values()) {
            session.setReady(false);
            session.setPlaying(false);
            session.setAlive(true);
            session.setCurrentScore(0D);
            session.setAngle(DEFAULT_ANGLE);
            session.setLastSeenAt(now);
        }
    }

    private List<OpenHexagonDto.PatternFrame> generatePatterns(long seed) {
        Random random = new Random(seed);
        List<OpenHexagonDto.PatternFrame> patterns = new ArrayList<>();
        int spawnAtMs = 900;

        for (int sequence = 0; sequence < PATTERN_COUNT; sequence += 1) {
            double difficulty = 1 + Math.min(spawnAtMs / 18000D, 4.6D);
            int intervalMs = (int) Math.max(210, 880 - difficulty * 80);

            String patternType = switch (random.nextInt(3)) {
                case 0 -> "gate";
                case 1 -> "zigzag";
                default -> "spiral";
            };

            int safeStart = random.nextInt(6);
            int ringCount = difficulty > 2.4D && random.nextDouble() > 0.52D ? 2 : 1;

            patterns.add(OpenHexagonDto.PatternFrame.builder()
                    .sequence(sequence)
                    .spawnAtMs(spawnAtMs)
                    .patternType(patternType)
                    .safeStart(safeStart)
                    .ringCount(ringCount)
                    .build());

            spawnAtMs += intervalMs;
        }

        return patterns;
    }

    private CosmeticProfile resolveCosmetics(Long userIdx) {
        Set<Integer> usedAvatars = new HashSet<>();
        Set<String> usedColors = new HashSet<>();

        for (PlayerSession session : sessions.values()) {
            if (session.getUserIdx() != null && !session.getUserIdx().equals(userIdx)) {
                usedAvatars.add(session.getAvatarIndex());
                usedColors.add(session.getAccentColor());
            }
        }

        int preferredAvatar = Math.floorMod(userIdx != null ? userIdx.intValue() : 0, AVATAR_COUNT);
        int avatarIndex = preferredAvatar;

        for (int step = 0; step < AVATAR_COUNT; step += 1) {
            int candidate = (preferredAvatar + step) % AVATAR_COUNT;
            if (!usedAvatars.contains(candidate)) {
                avatarIndex = candidate;
                break;
            }
        }

        int preferredColor = Math.floorMod(userIdx != null ? userIdx.intValue() : 0, DEFAULT_COLORS.size());
        String accentColor = DEFAULT_COLORS.get(preferredColor);

        for (int step = 0; step < DEFAULT_COLORS.size(); step += 1) {
            String candidate = DEFAULT_COLORS.get((preferredColor + step) % DEFAULT_COLORS.size());
            if (!usedColors.contains(candidate)) {
                accentColor = candidate;
                break;
            }
        }

        return new CosmeticProfile(avatarIndex, accentColor);
    }

    private String resolveNickname(AuthUserDetails user, String requestedNickname) {
        if (requestedNickname != null && !requestedNickname.isBlank()) {
            return requestedNickname.trim();
        }

        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }

        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }

        return "Pilot-" + user.getIdx();
    }

    private double normalizeScore(Double score) {
        if (score == null || score.isNaN() || score.isInfinite()) {
            return 0D;
        }
        return Math.max(0D, Math.min(score, 3600D));
    }

    private double normalizeAngle(Double angle) {
        if (angle == null || angle.isNaN() || angle.isInfinite()) {
            return DEFAULT_ANGLE;
        }

        double wrapped = angle % TAU;
        return wrapped >= 0 ? wrapped : wrapped + TAU;
    }

    private String format(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(FORMATTER);
    }

    private static class MatchRuntime {
        private String status = "LOBBY";
        private Integer roundNumber = 0;
        private Long roundSeed;
        private LocalDateTime countdownStartedAt;
        private LocalDateTime countdownEndsAt;
        private LocalDateTime roundStartsAt;
        private List<OpenHexagonDto.PatternFrame> patterns = List.of();

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getRoundNumber() {
            return roundNumber;
        }

        public void setRoundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
        }

        public Long getRoundSeed() {
            return roundSeed;
        }

        public void setRoundSeed(Long roundSeed) {
            this.roundSeed = roundSeed;
        }

        public LocalDateTime getCountdownStartedAt() {
            return countdownStartedAt;
        }

        public void setCountdownStartedAt(LocalDateTime countdownStartedAt) {
            this.countdownStartedAt = countdownStartedAt;
        }

        public LocalDateTime getCountdownEndsAt() {
            return countdownEndsAt;
        }

        public void setCountdownEndsAt(LocalDateTime countdownEndsAt) {
            this.countdownEndsAt = countdownEndsAt;
        }

        public LocalDateTime getRoundStartsAt() {
            return roundStartsAt;
        }

        public void setRoundStartsAt(LocalDateTime roundStartsAt) {
            this.roundStartsAt = roundStartsAt;
        }

        public List<OpenHexagonDto.PatternFrame> getPatterns() {
            return patterns;
        }

        public void setPatterns(List<OpenHexagonDto.PatternFrame> patterns) {
            this.patterns = patterns;
        }
    }

    private static class PlayerSession {
        private String sessionId;
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
        private LocalDateTime joinedAt;
        private LocalDateTime lastSeenAt;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public Long getUserIdx() {
            return userIdx;
        }

        public void setUserIdx(Long userIdx) {
            this.userIdx = userIdx;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getAvatarIndex() {
            return avatarIndex;
        }

        public void setAvatarIndex(Integer avatarIndex) {
            this.avatarIndex = avatarIndex;
        }

        public String getAccentColor() {
            return accentColor;
        }

        public void setAccentColor(String accentColor) {
            this.accentColor = accentColor;
        }

        public Double getAngle() {
            return angle;
        }

        public void setAngle(Double angle) {
            this.angle = angle;
        }

        public Boolean getReady() {
            return Boolean.TRUE.equals(ready);
        }

        public void setReady(Boolean ready) {
            this.ready = ready;
        }

        public Boolean getAlive() {
            return Boolean.TRUE.equals(alive);
        }

        public void setAlive(Boolean alive) {
            this.alive = alive;
        }

        public Boolean getPlaying() {
            return Boolean.TRUE.equals(playing);
        }

        public void setPlaying(Boolean playing) {
            this.playing = playing;
        }

        public Double getCurrentScore() {
            return currentScore == null ? 0D : currentScore;
        }

        public void setCurrentScore(Double currentScore) {
            this.currentScore = currentScore;
        }

        public LocalDateTime getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
        }

        public LocalDateTime getLastSeenAt() {
            return lastSeenAt;
        }

        public void setLastSeenAt(LocalDateTime lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
        }
    }

    private record ScoreRecord(
            Long userIdx,
            String nickname,
            String email,
            Integer avatarIndex,
            String accentColor,
            double bestScore,
            LocalDateTime updatedAt
    ) {
    }

    private record CosmeticProfile(
            Integer avatarIndex,
            String accentColor
    ) {
    }
}
