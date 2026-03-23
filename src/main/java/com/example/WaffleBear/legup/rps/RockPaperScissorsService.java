package com.example.WaffleBear.legup.rps;

import com.example.WaffleBear.user.model.AuthUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RockPaperScissorsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_CHAT_MESSAGES = 40;
    private static final List<String> CHOICES = List.of("rock", "paper", "scissors");

    private final Map<String, RoomRuntime> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRoomIndex = new ConcurrentHashMap<>();

    public synchronized RockPaperScissorsDto.RoomState join(String sessionId, AuthUserDetails user, RockPaperScissorsDto.JoinRequest request) {
        String roomId = normalizeRoomId(request == null ? null : request.roomId());
        leaveInternal(sessionId, false);

        RoomRuntime room = rooms.computeIfAbsent(roomId, RoomRuntime::new);
        PlayerRuntime player = room.players.get(user.getIdx());

        if (player == null && room.players.size() >= 2) {
            throw new IllegalStateException("현재 방은 가득 찼습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (player == null) {
            player = new PlayerRuntime();
            player.userIdx = user.getIdx();
            player.joinedAt = now;
            room.players.put(user.getIdx(), player);
            appendSystemMessage(room, resolveNickname(user) + "님이 입장했습니다.", now);
        }

        player.sessionId = sessionId;
        player.email = user.getEmail();
        player.nickname = resolveNickname(user);
        player.connected = true;
        player.lastSeenAt = now;

        sessionRoomIndex.put(sessionId, roomId);
        room.updatedAt = now;
        return toRoomState(room);
    }

    public synchronized RockPaperScissorsDto.RoomState sendChat(String sessionId, AuthUserDetails user, RockPaperScissorsDto.ChatRequest request) {
        RoomRuntime room = requireRoom(sessionId, request == null ? null : request.roomId());
        PlayerRuntime player = requirePlayer(room, user.getIdx());
        String message = normalizeMessage(request == null ? null : request.message());

        room.messages.add(new MessageRuntime(player.userIdx, player.nickname, message, LocalDateTime.now()));
        trimMessages(room);
        if (room.typingUserIdx != null && room.typingUserIdx.equals(player.userIdx)) {
            room.typingUserIdx = null;
        }
        room.updatedAt = LocalDateTime.now();
        return toRoomState(room);
    }

    public synchronized RockPaperScissorsDto.RoomState updateTyping(String sessionId, AuthUserDetails user, RockPaperScissorsDto.TypingRequest request) {
        RoomRuntime room = requireRoom(sessionId, request == null ? null : request.roomId());
        PlayerRuntime player = requirePlayer(room, user.getIdx());

        if (Boolean.TRUE.equals(request == null ? null : request.typing())) {
            room.typingUserIdx = player.userIdx;
        } else if (room.typingUserIdx != null && room.typingUserIdx.equals(player.userIdx)) {
            room.typingUserIdx = null;
        }

        player.lastSeenAt = LocalDateTime.now();
        room.updatedAt = LocalDateTime.now();
        return toRoomState(room);
    }

    public synchronized RockPaperScissorsDto.RoomState submitChoice(String sessionId, AuthUserDetails user, RockPaperScissorsDto.ChoiceRequest request) {
        RoomRuntime room = requireRoom(sessionId, request == null ? null : request.roomId());
        PlayerRuntime player = requirePlayer(room, user.getIdx());
        player.choice = normalizeChoice(request == null ? null : request.choice());
        player.lastSeenAt = LocalDateTime.now();
        if (room.typingUserIdx != null && room.typingUserIdx.equals(player.userIdx)) {
            room.typingUserIdx = null;
        }

        evaluateRound(room);
        room.updatedAt = LocalDateTime.now();
        return toRoomState(room);
    }

    public synchronized RockPaperScissorsDto.RoomState resetRound(String sessionId, AuthUserDetails user, RockPaperScissorsDto.ResetRequest request) {
        RoomRuntime room = requireRoom(sessionId, request == null ? null : request.roomId());
        requirePlayer(room, user.getIdx());

        room.result = null;
        room.typingUserIdx = null;
        for (PlayerRuntime player : room.players.values()) {
            player.choice = null;
            player.lastSeenAt = LocalDateTime.now();
        }

        room.updatedAt = LocalDateTime.now();
        return toRoomState(room);
    }

    public synchronized RockPaperScissorsDto.LeaveOutcome leave(String sessionId) {
        return leaveInternal(sessionId, true);
    }

    private RockPaperScissorsDto.LeaveOutcome leaveInternal(String sessionId, boolean announce) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        String roomId = sessionRoomIndex.remove(sessionId);
        if (!StringUtils.hasText(roomId)) {
            return null;
        }

        RoomRuntime room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        PlayerRuntime removedPlayer = room.players.values().stream()
                .filter(player -> sessionId.equals(player.sessionId))
                .findFirst()
                .orElse(null);

        if (removedPlayer != null) {
            room.players.remove(removedPlayer.userIdx);
            if (announce) {
                appendSystemMessage(room, removedPlayer.nickname + "님이 퇴장했습니다.", LocalDateTime.now());
            }
        }

        if (room.typingUserIdx != null && removedPlayer != null && room.typingUserIdx.equals(removedPlayer.userIdx)) {
            room.typingUserIdx = null;
        }

        if (room.players.size() < 2) {
            room.result = null;
            for (PlayerRuntime player : room.players.values()) {
                player.choice = null;
            }
        }

        room.updatedAt = LocalDateTime.now();

        if (room.players.isEmpty()) {
            rooms.remove(roomId);
        }

        return new RockPaperScissorsDto.LeaveOutcome(roomId, toRoomState(room));
    }

    private RoomRuntime requireRoom(String sessionId, String requestedRoomId) {
        String roomId = sessionRoomIndex.get(sessionId);
        if (!StringUtils.hasText(roomId)) {
            roomId = normalizeRoomId(requestedRoomId);
        }

        RoomRuntime room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalStateException("참여 중인 방을 찾을 수 없습니다.");
        }

        return room;
    }

    private PlayerRuntime requirePlayer(RoomRuntime room, Long userIdx) {
        PlayerRuntime player = room.players.get(userIdx);
        if (player == null) {
            throw new IllegalStateException("방에 참여한 사용자만 게임을 진행할 수 있습니다.");
        }
        return player;
    }

    private void evaluateRound(RoomRuntime room) {
        if (room.players.size() < 2) {
            room.result = null;
            return;
        }

        List<PlayerRuntime> players = room.players.values().stream()
                .sorted(Comparator.comparing(player -> player.joinedAt))
                .toList();

        PlayerRuntime first = players.get(0);
        PlayerRuntime second = players.get(1);
        if (!StringUtils.hasText(first.choice) || !StringUtils.hasText(second.choice)) {
            room.result = null;
            return;
        }

        if (first.choice.equals(second.choice)) {
            room.result = new ResultRuntime("DRAW", null, null, first.choice, null, null, second.choice);
            return;
        }

        PlayerRuntime winner = isWinner(first.choice, second.choice) ? first : second;
        PlayerRuntime loser = winner == first ? second : first;
        winner.winCount += 1;
        room.result = new ResultRuntime(
                "WIN",
                winner.userIdx,
                winner.nickname,
                winner.choice,
                loser.userIdx,
                loser.nickname,
                loser.choice
        );
    }

    private boolean isWinner(String leftChoice, String rightChoice) {
        return ("rock".equals(leftChoice) && "scissors".equals(rightChoice))
                || ("paper".equals(leftChoice) && "rock".equals(rightChoice))
                || ("scissors".equals(leftChoice) && "paper".equals(rightChoice));
    }

    private String normalizeRoomId(String roomId) {
        String normalized = roomId == null ? "" : roomId.trim();
        normalized = normalized.replaceAll("[^0-9A-Za-z_-]", "");
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalStateException("방 코드를 입력해 주세요.");
        }
        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
    }

    private String normalizeChoice(String choice) {
        String normalized = choice == null ? "" : choice.trim().toLowerCase(Locale.ROOT);
        if (!CHOICES.contains(normalized)) {
            throw new IllegalStateException("선택값이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeMessage(String message) {
        String normalized = message == null ? "" : message.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalStateException("메시지를 입력해 주세요.");
        }
        return normalized.length() > 300 ? normalized.substring(0, 300) : normalized;
    }

    private String resolveNickname(AuthUserDetails user) {
        String name = user.getName();
        if (StringUtils.hasText(name)) {
            return name.trim();
        }

        String email = user.getEmail();
        if (!StringUtils.hasText(email)) {
            return "Player";
        }

        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private void appendSystemMessage(RoomRuntime room, String message, LocalDateTime now) {
        room.messages.add(new MessageRuntime(null, "system", message, now));
        trimMessages(room);
    }

    private void trimMessages(RoomRuntime room) {
        while (room.messages.size() > MAX_CHAT_MESSAGES) {
            room.messages.remove(0);
        }
    }

    private RockPaperScissorsDto.RoomState toRoomState(RoomRuntime room) {
        if (room == null) {
            return new RockPaperScissorsDto.RoomState("", "WAITING", List.of(), List.of(), null, null, format(LocalDateTime.now()));
        }

        boolean revealChoices = room.result != null;
        List<RockPaperScissorsDto.PlayerView> players = room.players.values().stream()
                .sorted(Comparator.comparing(player -> player.joinedAt))
                .map(player -> new RockPaperScissorsDto.PlayerView(
                        player.userIdx,
                        player.nickname,
                        player.email,
                        player.connected,
                        StringUtils.hasText(player.choice),
                        revealChoices ? player.choice : null,
                        player.winCount
                ))
                .toList();

        List<RockPaperScissorsDto.MessageView> messages = room.messages.stream()
                .map(message -> new RockPaperScissorsDto.MessageView(
                        message.userIdx,
                        message.nickname,
                        message.message,
                        format(message.createdAt)
                ))
                .toList();

        String typingNickname = null;
        if (room.typingUserIdx != null) {
            PlayerRuntime typingPlayer = room.players.get(room.typingUserIdx);
            if (typingPlayer != null) {
                typingNickname = typingPlayer.nickname;
            }
        }

        RockPaperScissorsDto.ResultView result = room.result == null
                ? null
                : new RockPaperScissorsDto.ResultView(
                        room.result.outcome,
                        room.result.winnerUserIdx,
                        room.result.winnerNickname,
                        room.result.winnerChoice,
                        room.result.loserUserIdx,
                        room.result.loserNickname,
                        room.result.loserChoice
                );

        return new RockPaperScissorsDto.RoomState(
                room.roomId,
                resolveStatus(room),
                players,
                messages,
                typingNickname,
                result,
                format(room.updatedAt)
        );
    }

    private String resolveStatus(RoomRuntime room) {
        if (room == null || room.players.size() < 2) {
            return "WAITING";
        }

        if (room.result != null) {
            return "RESULT";
        }

        boolean anyChoiceLocked = room.players.values().stream().anyMatch(player -> StringUtils.hasText(player.choice));
        return anyChoiceLocked ? "PLAYING" : "READY";
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }

    private static final class RoomRuntime {
        private final String roomId;
        private final Map<Long, PlayerRuntime> players = new LinkedHashMap<>();
        private final List<MessageRuntime> messages = new ArrayList<>();
        private Long typingUserIdx;
        private ResultRuntime result;
        private LocalDateTime updatedAt = LocalDateTime.now();

        private RoomRuntime(String roomId) {
            this.roomId = roomId;
        }
    }

    private static final class PlayerRuntime {
        private String sessionId;
        private Long userIdx;
        private String nickname;
        private String email;
        private Boolean connected = true;
        private String choice;
        private Integer winCount = 0;
        private LocalDateTime joinedAt;
        private LocalDateTime lastSeenAt;
    }

    private record MessageRuntime(Long userIdx, String nickname, String message, LocalDateTime createdAt) {}

    private record ResultRuntime(
            String outcome,
            Long winnerUserIdx,
            String winnerNickname,
            String winnerChoice,
            Long loserUserIdx,
            String loserNickname,
            String loserChoice
    ) {}
}
