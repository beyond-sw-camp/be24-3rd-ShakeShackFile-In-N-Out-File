package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatMessagesDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.feater.FeaterService;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationService notificationService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FeaterService featerService;

    @Transactional(readOnly = true)
    public ChatMessagesDto.PageRes getMessageList(Long roomIdx, Long userIdx, int page, int size) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        boolean isParticipant = participantsRepository.existsByChatRoomsIdxAndUsersIdx(room.getIdx(), userIdx);
        if (!isParticipant) throw new RuntimeException("해당 채팅방에 접근 권한이 없습니다.");

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessages> result = chatMessageRepository.findAllByChatRooms(room, pageable);

        // 각 메시지마다 readCount 계산
        List<ChatMessagesDto.ListRes> messageList = result.getContent().stream()
                .map(msg -> {
                    int messageUnreadCount = chatMessageRepository.countUnreadParticipants(
                            roomIdx, msg.getIdx(), msg.getSender().getIdx()
                    );
                    String profileImageUrl = featerService.resolveProfileImage(msg.getSender().getIdx());
                    return ChatMessagesDto.ListRes.from(msg, messageUnreadCount, profileImageUrl);
                })
                .toList();

        return ChatMessagesDto.PageRes.builder()
                .messageList(messageList)
                .totalPage(result.getTotalPages())
                .totalCount(result.getTotalElements())
                .currentPage(result.getPageable().getPageNumber())
                .currentSize(result.getPageable().getPageSize())
                .build();
    }

    @Transactional
    public ChatMessagesDto.ListRes saveMessage(Long roomIdx, ChatMessagesDto.Send req, Long senderIdx) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        User user = userRepository.findById(senderIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        ChatMessages message = chatMessageRepository.save(req.toEntity(room, user));
        room.updateLastMessage(message.getContents(), message.getCreatedAt());

        // 발신자 자동 읽음 처리
        participantsRepository.findByChatRoomsIdxAndUsersIdx(roomIdx, senderIdx)
                .ifPresent(p -> p.updateLastReadMessageId(message.getIdx()));

        List<ChatParticipants> participants = participantsRepository.findAllByChatRoomsIdx(roomIdx);
        for (ChatParticipants participant : participants) {
            Long userIdx = participant.getUsers().getIdx();
            if (!userIdx.equals(senderIdx)) {
                if (!chatRoomService.isActiveInRoom(roomIdx, userIdx)) {
                    ChatParticipants p = participantsRepository
                            .findByChatRoomsIdxAndUsersIdx(roomIdx, userIdx)
                            .orElse(null);
                    long unreadCount = p != null
                            ? chatMessageRepository.countByChatRoomsIdxAndIdxGreaterThan(
                            roomIdx,
                            p.getLastReadMessageId() != null ? p.getLastReadMessageId() : 0L)
                            : 0L;
                    notificationService.sendToUser(userIdx, room.getTitle(),
                            user.getName() + ": " + message.getContents(), roomIdx, unreadCount);
                }
            }
        }

        int messageUnreadCount = chatMessageRepository.countUnreadParticipants(
                roomIdx, message.getIdx(), senderIdx
        );
        return ChatMessagesDto.ListRes.from(message, messageUnreadCount,null);
    }

    @Transactional
    public void markAsRead(Long roomIdx, Long userIdx) {
        ChatParticipants participant = participantsRepository
                .findByChatRoomsIdxAndUsersIdx(roomIdx, userIdx)
                .orElseThrow(() -> new RuntimeException("참여자 정보 없음"));

        chatMessageRepository.findTopByChatRoomsIdxOrderByCreatedAtDesc(roomIdx)
                .ifPresent(msg -> {
                    participant.updateLastReadMessageId(msg.getIdx());

                    // 읽음 이벤트 브로드캐스트 → 방 안의 모든 사람 화면 갱신
                    messagingTemplate.convertAndSend(
                            "/sub/chat/room/" + roomIdx,
                            Map.of("type", "READ_UPDATE", "userIdx", userIdx)
                    );
                });
    }
}