package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatParticipantsDto;
import com.example.WaffleBear.chat.model.dto.ChatRoomsDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.chat.model.entity.MessageType;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {
    private static final LocalDateTime DEFAULT_JOINED_AT = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           UserRepository userRepository,
                           ParticipantsRepository participantsRepository,
                           ChatMessageRepository chatMessageRepository,
                           @Lazy SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.participantsRepository = participantsRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private final Map<Long, Set<Long>> activeUsers = new ConcurrentHashMap<>();

    private void sendSystemMessage(Long roomIdx, String text, MessageType type) {
        Map<String, Object> payload = Map.of(
                "roomIdx", roomIdx,
                "contents", text,
                "messageType", type.name(),
                "createdAt", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomIdx, payload);
    }

    @Transactional
    public Long createChatRoom(ChatRoomsDto.ChatRoomsReq dto, Long myIdx) {
        ChatRooms room = chatRoomRepository.save(dto.toEntity());

        Set<User> invitees = new HashSet<>();
        invitees.add(userRepository.findById(myIdx)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다.")));

        if (dto.getParticipantsEmail() != null) {
            for (String email : dto.getParticipantsEmail()) {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다. " + email));
                invitees.add(user);
            }
        }

        this.addParticipantsToRoom(room, invitees);
        return room.getIdx();
    }

    @Transactional
    public void inviteUsersByEmail(Long roomId, List<String> emails) {
        ChatRooms room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        List<User> foundUsers = userRepository.findAllByEmailIn(emails);

        if (foundUsers.size() != emails.size()) {
            Set<String> foundEmails = foundUsers.stream()
                    .map(User::getEmail)
                    .collect(Collectors.toSet());

            String missingEmails = emails.stream()
                    .filter(email -> !foundEmails.contains(email))
                    .collect(Collectors.joining(", "));

            throw new RuntimeException("존재하지 않는 이메일이 포함되어 있습니다: " + missingEmails);
        }

        this.addParticipantsToRoom(room, new HashSet<>(foundUsers));
        for (User user : foundUsers) {
            sendSystemMessage(roomId, user.getName() + "님이 입장했습니다.", MessageType.ENTER);
        }
    }

    @Transactional
    public int inviteUsers(Long roomId, Long actorUserIdx, Collection<Long> userIds) {
        ChatRooms room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        if (!participantsRepository.existsByChatRoomsIdxAndUsersIdx(roomId, actorUserIdx)) {
            throw new IllegalArgumentException("채팅방 초대 권한이 없습니다.");
        }

        List<User> targetUsers = userIds == null
                ? List.of()
                : userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(userId -> userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. idx: " + userId)))
                .filter(user -> !Objects.equals(user.getIdx(), actorUserIdx))
                .toList();

        Set<User> newUsers = targetUsers.stream()
                .filter(user -> participantsRepository.findByChatRoomsIdxAndUsersIdx(roomId, user.getIdx()).isEmpty())
                .collect(Collectors.toSet());

        if (newUsers.isEmpty()) {
            return 0;
        }

        this.addParticipantsToRoom(room, newUsers);
        newUsers.forEach(user ->
                sendSystemMessage(roomId, user.getName() + "님이 입장했습니다.", MessageType.ENTER)
        );
        return newUsers.size();
    }

    private void addParticipantsToRoom(ChatRooms room, Set<User> users) {
        for (User user : users) {
            participantsRepository.findByChatRoomsIdxAndUsersIdx(room.getIdx(), user.getIdx())
                    .ifPresentOrElse(
                            existing -> {
                                existing.updateJoinedAt();
                                participantsRepository.save(existing);
                            },
                            () -> participantsRepository.save(ChatParticipantsDto.Create.toEntity(room, user))
                    );
        }
    }

    public ChatRoomsDto.PageRes list(int page, int size, Long userIdx) {
        Page<ChatParticipants> result = participantsRepository.findAllByUsersIdx(
                userIdx,
                PageRequest.of(page, size)
        );

        List<ChatParticipants> pageContent = result.getContent();
        if (pageContent.isEmpty()) {
            return ChatRoomsDto.PageRes.from(result, Map.of(), Map.of(), Map.of());
        }

        Map<Long, Long> unreadMap = pageContent.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getChatRooms().getIdx(),
                        participant -> {
                            Long lastReadId = participant.getLastReadMessageId() != null
                                    ? participant.getLastReadMessageId()
                                    : 0L;
                            LocalDateTime joinedAt = participant.getJoinedAt() != null
                                    ? participant.getJoinedAt()
                                    : DEFAULT_JOINED_AT;

                            return chatMessageRepository.countByChatRoomsIdxAndIdxGreaterThanAndCreatedAtAfter(
                                    participant.getChatRooms().getIdx(),
                                    lastReadId,
                                    joinedAt
                            );
                        }
                ));

        Map<Long, String> lastMessageMap = pageContent.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getChatRooms().getIdx(),
                        participant -> {
                            LocalDateTime joinedAt = participant.getJoinedAt() != null
                                    ? participant.getJoinedAt()
                                    : DEFAULT_JOINED_AT;

                            return chatMessageRepository
                                    .findTopByChatRoomsIdxAndCreatedAtAfterOrderByCreatedAtDesc(
                                            participant.getChatRooms().getIdx(),
                                            joinedAt
                                    )
                                    .map(this::getPreviewMessage)
                                    .orElse("");
                        }
                ));

        Map<Long, Integer> participantCountMap = participantsRepository.countParticipantsByRoomIds(
                        pageContent.stream()
                                .map(participant -> participant.getChatRooms().getIdx())
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        return ChatRoomsDto.PageRes.from(result, unreadMap, lastMessageMap, participantCountMap);
    }

    private String getPreviewMessage(ChatMessages message) {
        if (message == null) return "메시지가 없습니다.";
        if (message.isDeleted()) return "삭제된 메시지입니다.";
        if (message.getMessageType() == MessageType.IMAGE) return "사진";
        if (message.getMessageType() == MessageType.FILE) return "문서";

        String contents = message.getContents();
        return (contents == null || contents.isBlank()) ? "메시지가 없습니다." : contents;
    }

    @Transactional
    public void exit(Long roomIdx, Long userIdx) {
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. idx: " + userIdx));
        String name = user.getName();

        participantsRepository.deleteByChatRoomsIdxAndUsersIdx(roomIdx, userIdx);
        participantsRepository.flush();

        if (!participantsRepository.existsByChatRoomsIdx(roomIdx)) {
            chatMessageRepository.deleteAllByChatRoomsIdx(roomIdx);
            chatMessageRepository.flush();
            chatRoomRepository.deleteById(roomIdx);
        } else {
            sendSystemMessage(roomIdx, name + "님이 채팅방을 나갔습니다.", MessageType.EXIT);
        }
    }

    public boolean isMember(Long roomId, Long userIdx) {
        return participantsRepository.existsByChatRoomsIdxAndUsersIdx(roomId, userIdx);
    }

    @Transactional
    public void updateRoomTitle(Long roomIdx, String newTitle, Long userIdx) {
        ChatParticipants participant = participantsRepository.findByChatRoomsIdxAndUsersIdx(roomIdx, userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방 참여 정보를 찾을 수 없습니다."));

        participant.setCustomRoomName(newTitle);
    }

    public void enterRoom(Long roomIdx, Long userIdx) {
        activeUsers.computeIfAbsent(roomIdx, k -> ConcurrentHashMap.newKeySet()).add(userIdx);
    }

    public void leaveRoom(Long roomIdx, Long userIdx) {
        Set<Long> users = activeUsers.get(roomIdx);
        if (users != null) {
            users.remove(userIdx);
        }
    }

    public boolean isActiveInRoom(Long roomIdx, Long userIdx) {
        Set<Long> users = activeUsers.get(roomIdx);
        return users != null && users.contains(userIdx);
    }
}
