package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatParticipantsDto;
import com.example.WaffleBear.chat.model.dto.ChatRoomsDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.chat.model.entity.MessageType;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.user.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService chatPresenceService;
    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           UserRepository userRepository,
                           ParticipantsRepository participantsRepository,
                           ChatMessageRepository chatMessageRepository,
                           @Lazy SimpMessagingTemplate messagingTemplate,
                            ChatPresenceService chatPresenceService) {
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.participantsRepository = participantsRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.chatPresenceService = chatPresenceService;
    }


    private void sendSystemMessage(Long roomIdx, String text, MessageType type) {
        Map<String, Object> payload = Map.of(
                "roomIdx",     roomIdx,
                "contents",    text,
                "messageType", type.name(),
                "createdAt",   LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomIdx, payload);
    }

        // 1. 방 생성 (내부에 초대 로직 포함)
        @Transactional
        public Long createChatRoom(ChatRoomsDto.ChatRoomsReq dto, Long myIdx) {
            // 방 엔티티 생성 및 저장
            ChatRooms room = chatRoomRepository.save(dto.toEntity());

            // 이메일 리스트를 User 객체 리스트로 변환 (유효성 검사 포함)
            Set<User> invitees = new HashSet<>();

            // 내 정보 추가
            invitees.add(userRepository.findById(myIdx)
                    .orElseThrow(() -> new RuntimeException("내 정보를 찾을 수 없습니다.")));

            // 초대받은 이메일들 처리
            if (dto.getParticipantsEmail() != null) {
                for (String email : dto.getParticipantsEmail()) {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다: " + email));
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

        // 1. DB에서 이메일에 해당하는 유저들을 한꺼번에 가져옴 (쿼리 1번)
        List<User> foundUsers = userRepository.findAllByEmailIn(emails);

        // 2. 입력받은 이메일 개수와 DB에서 찾은 유저 수가 다르면 없는 유저가 섞여 있는 것
        if (foundUsers.size() != emails.size()) {
            // 어떤 이메일이 없는지 찾아서 알려주면 더 친절함
            Set<String> foundEmails = foundUsers.stream()
                    .map(User::getEmail)
                    .collect(Collectors.toSet());

            String missingEmails = emails.stream()
                    .filter(e -> !foundEmails.contains(e))
                    .collect(Collectors.joining(", "));

            throw new RuntimeException("존재하지 않는 유저가 포함되어 있습니다: " + missingEmails);
        }

        // 3. 공통 메서드로 전달 (이미 Set<User> 형태이므로 중복 자동 제거)
        this.addParticipantsToRoom(room, new HashSet<>(foundUsers));
        for (User user : foundUsers) {
            sendSystemMessage(roomId, user.getName() + "님이 입장했습니다.", MessageType.ENTER);
        }
    }

    // [개선된 공통 로직] 중복 방지 강화
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
                                existing.updateJoinedAt(); // ✅ 재입장 시 joinedAt 갱신
                                participantsRepository.save(existing);
                            },
                            () -> {
                                participantsRepository.save(ChatParticipantsDto.Create.toEntity(room, user));
                            }
                    );
        }
    }


    public ChatRoomsDto.PageRes list(int page, int size, Long userIdx) {
        List<ChatParticipants> sorted = participantsRepository.findAllByUsersIdx(userIdx)
                .stream()
                .sorted((a, b) -> {
                    LocalDateTime timeA = a.getChatRooms().getLastMessageTime();
                    LocalDateTime timeB = b.getChatRooms().getLastMessageTime();
                    if (timeA == null) return 1;
                    if (timeB == null) return -1;
                    return timeB.compareTo(timeA);
                })
                .toList();
        Map<Long, Long> unreadMap = sorted.stream()
                .collect(Collectors.toMap(
                        p -> p.getChatRooms().getIdx(),
                        p -> {// ✅ lastReadMessageId 기준 AND joinedAt 이후 메시지만 카운트
                                Long lastReadId = p.getLastReadMessageId() != null ? p.getLastReadMessageId() : 0L;
        LocalDateTime joinedAt = p.getJoinedAt() != null
                ? p.getJoinedAt()
                : LocalDateTime.of(2000, 1, 1, 0, 0);

        return chatMessageRepository.countByChatRoomsIdxAndIdxGreaterThanAndCreatedAtAfter(
                p.getChatRooms().getIdx(),
                lastReadId,
                joinedAt
        );
    }
            ));

        // ✅ joinedAt 이후 메시지가 있는지 확인해서 lastMessage 덮어쓰기
        Map<Long, String> lastMessageMap = sorted.stream()
                .collect(Collectors.toMap(
                        p -> p.getChatRooms().getIdx(),
                        p -> {
                            LocalDateTime joinedAt = p.getJoinedAt() != null
                                    ? p.getJoinedAt()
                                    : LocalDateTime.of(2000, 1, 1, 0, 0);
                            return chatMessageRepository
                                    .findTopByChatRoomsIdxAndCreatedAtAfterOrderByCreatedAtDesc(
                                            p.getChatRooms().getIdx(), joinedAt)
                                    .map(this::getPreviewMessage)
                                    .orElse(""); // ✅ joinedAt 이전 메시지면 빈 문자열
                        }
                ));

    int start = page * size;
    int end = Math.min(start + size, sorted.size());
    List<ChatParticipants> paged = sorted.subList(start, end);
    Page<ChatParticipants> result = new PageImpl<>(paged, PageRequest.of(page, size), sorted.size());

    return ChatRoomsDto.PageRes.from(result, unreadMap, lastMessageMap);
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
        // 1. 해당 방에서 내가 참여자인지 확인하고 삭제
        participantsRepository.deleteByChatRoomsIdxAndUsersIdx(roomIdx, userIdx);
        participantsRepository.flush();
        // 2. 방에 남은 사람이 더 이상 없는지 확인
        if (!participantsRepository.existsByChatRoomsIdx(roomIdx)) {
            // 3. (중요) 해당 방의 메시지를 먼저 삭제해야 외래키 오류가 안 남
            chatMessageRepository.deleteAllByChatRoomsIdx(roomIdx);
            chatMessageRepository.flush(); // DB에 즉시 반영
            // 4. 마지막으로 방 삭제
            chatRoomRepository.deleteById(roomIdx);
        }else {
            // ✅ 방이 아직 존재할 때만 퇴장 메시지 전송
            sendSystemMessage(roomIdx, name + "님이 채팅방을 떠났습니다.", MessageType.EXIT);
        }
    }
    public boolean isMember(Long roomId, Long userIdx) {
        return participantsRepository.existsByChatRoomsIdxAndUsersIdx(roomId, userIdx);
    }

    @Transactional // 트랜잭션을 통해 Dirty Checking으로 저장합니다.
    public void updateRoomTitle(Long roomIdx, String newTitle, Long userIdx) {
        ChatParticipants participant = participantsRepository.findByChatRoomsIdxAndUsersIdx(roomIdx, userIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방 참여 정보를 찾을 수 없습니다."));

        participant.setCustomRoomName(newTitle);
    }
    public void enterRoom(Long roomIdx, Long userIdx) {
        chatPresenceService.enter(roomIdx, userIdx);
    }
    public void leaveRoom(Long roomIdx, Long userIdx) {
        chatPresenceService.leave(roomIdx, userIdx);
    }
    public boolean isActiveInRoom(Long roomIdx, Long userIdx) {
        return chatPresenceService.isActiveInRoom(roomIdx, userIdx);
    }

}
