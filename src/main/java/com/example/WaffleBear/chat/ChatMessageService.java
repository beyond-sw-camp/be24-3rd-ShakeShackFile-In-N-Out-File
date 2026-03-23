package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatMessagesDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.config.sse.SseService;
import com.example.WaffleBear.feater.FeaterService;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final SseService sseService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationService notificationService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FeaterService featerService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;   // 5MB
    private static final long MAX_FILE_SIZE = 30L * 1024 * 1024;   // 30MB
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg"
    );

    @Transactional(readOnly = true)
    public ChatMessagesDto.PageRes getMessageList(Long roomIdx, Long userIdx, int page, int size) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        // ✅ 참여자 정보에서 joinedAt 가져오기
        ChatParticipants participant = participantsRepository
                .findByChatRoomsIdxAndUsersIdx(roomIdx, userIdx)
                .orElseThrow(() -> new RuntimeException("해당 채팅방에 접근 권한이 없습니다."));

        LocalDateTime joinedAt = participant.getJoinedAt() != null
                ? participant.getJoinedAt()
                : LocalDateTime.of(2000, 1, 1, 0, 0); // null 방어용 기본값

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessages> result = chatMessageRepository
                .findAllByChatRoomsAndCreatedAtAfter(room, joinedAt, pageable);

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
        ChatRooms room = chatRoomRepository.findByIdWithLock(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        User user = userRepository.findById(senderIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        ChatMessages message = chatMessageRepository.save(req.toEntity(room, user));
        room.updateLastMessage(message.getContents(), message.getCreatedAt());

        List<ChatParticipants> participants = participantsRepository.findAllByChatRoomsIdx(roomIdx);

        for (ChatParticipants participant : participants) {
            Long userIdx = participant.getUsers().getIdx();

            if (userIdx.equals(senderIdx)) {
                // 발신자 읽음 처리 (별도 조회 불필요)
                participant.updateLastReadMessageId(message.getIdx());
                continue;
            }

            if (!chatRoomService.isActiveInRoom(roomIdx, userIdx)) {
                long unreadCount = chatMessageRepository.countByChatRoomsIdxAndIdxGreaterThan(
                        roomIdx,
                        participant.getLastReadMessageId() != null ? participant.getLastReadMessageId() : 0L
                );
                notificationService.sendToUser(userIdx, room.getTitle(),
                        user.getName() + ": " + message.getContents(), roomIdx, unreadCount);
            }
        }

        String profileImageUrl = featerService.resolveProfileImage(senderIdx);
        int messageUnreadCount = chatMessageRepository.countUnreadParticipants(roomIdx, message.getIdx(), senderIdx);
        return ChatMessagesDto.ListRes.from(message, messageUnreadCount, profileImageUrl);
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

    public String uploadFile(Long roomIdx, MultipartFile file, Long userIdx) {
        String contentType = file.getContentType();
        boolean isImage = IMAGE_TYPES.contains(contentType);

        // 용량 체크
        if (isImage && file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 업로드 가능합니다.");
        }
        if (!isImage && file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일은 30MB 이하만 업로드 가능합니다.");
        }

        try {
            String objectKey = "chat/" + roomIdx + "/" + userIdx + "/"
                    + System.currentTimeMillis() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            // presigned URL 반환
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .expiry(60 * 60 * 24) // 24시간
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage());
        }
    }
    @Transactional
    public void deleteMessage(Long roomIdx, Long messageIdx, Long userIdx) {
        ChatMessages message = chatMessageRepository.findByIdxAndChatRoomsIdx(messageIdx, roomIdx)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));

        if (!message.getSender().getIdx().equals(userIdx)) {
            throw new IllegalArgumentException("본인 메시지만 삭제할 수 있습니다.");
        }

        message.markDeleted();

        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + roomIdx,
                Map.of(
                        "type", "MESSAGE_DELETED",
                        "roomIdx", roomIdx,
                        "messageIdx", messageIdx,
                        "contents", message.getContents(),
                        "messageType", "TEXT"
                )
        );
        sendChatPreviewUpdate(roomIdx);
    }
    private void sendChatPreviewUpdate(Long roomIdx) {
        List<ChatParticipants> participants = participantsRepository.findAllByChatRoomsIdx(roomIdx);

        for (ChatParticipants participant : participants) {
            Long userIdx = participant.getUsers().getIdx();

            LocalDateTime joinedAt = participant.getJoinedAt() != null
                    ? participant.getJoinedAt()
                    : LocalDateTime.of(2000, 1, 1, 0, 0);

            Long lastReadId = participant.getLastReadMessageId() != null
                    ? participant.getLastReadMessageId()
                    : 0L;

            String lastMsg = chatMessageRepository
                    .findTopByChatRoomsIdxAndCreatedAtAfterOrderByCreatedAtDesc(roomIdx, joinedAt)
                    .map(ChatMessages::getContents)
                    .orElse("메시지가 없습니다.");

            long unreadCount = chatMessageRepository.countByChatRoomsIdxAndIdxGreaterThanAndCreatedAtAfter(
                    roomIdx,
                    lastReadId,
                    joinedAt
            );

            Map<String, Object> payload = Map.of(
                    "roomIdx", roomIdx,
                    "lastMsg", lastMsg,
                    "unreadCount", unreadCount
            );

            sseService.sendEventToUser(userIdx, "chat-preview-update", payload);
        }
    }

}