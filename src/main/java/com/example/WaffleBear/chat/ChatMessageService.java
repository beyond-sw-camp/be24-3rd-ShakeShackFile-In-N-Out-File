package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatMessagesDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ChatMessagesDto.PageRes getMessageList(Long roomIdx, Long userIdx, int page, int size) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        boolean isParticipant = participantsRepository.existsByChatRoomsIdxAndUsersIdx(room.getIdx(), userIdx);

        if (!isParticipant) {
            throw new RuntimeException("해당 채팅방에 접근 권한이 없습니다.");
        }
        // Pageable 임포트 오류 해결 및 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessages> result = chatMessageRepository.findAllByChatRooms(room, pageable);

        return ChatMessagesDto.PageRes.from(result);
    }

    @Transactional
    public ChatMessagesDto.ListRes saveMessage(Long roomIdx, ChatMessagesDto.Send req, Long senderIdx) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        User user = userRepository.findById(senderIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 메시지 생성 및 저장
        ChatMessages message = chatMessageRepository.save(req.toEntity(room, user));

        // ChatRooms 정보 업데이트
        room.updateLastMessage(message.getContents(),message.getCreatedAt());

        List<ChatParticipants> participants = participantsRepository.findAllByChatRoomsIdx(roomIdx);
        for (ChatParticipants participant : participants) {
            Long userIdx = participant.getUsers().getIdx();
            System.out.println("알림 대상 userIdx: " + userIdx + ", 발신자: " + senderIdx);
            if (!userIdx.equals(senderIdx)) {
                notificationService.sendToUser(
                        userIdx,
                        room.getTitle(),  // 알림 제목: 채팅방 이름
                        user.getName() + ": " + message.getContents() // 알림 내용
                );
            }
        }
        // 전송용 응답 DTO 반환
        return ChatMessagesDto.ListRes.from(message);
    }
    @Transactional
    public void markAsRead(Long roomIdx, Long userIdx) {
        ChatParticipants participant = participantsRepository
                .findByChatRoomsIdxAndUsersIdx(roomIdx, userIdx)
                .orElseThrow(() -> new RuntimeException("참여자 정보 없음"));

        // 해당 방의 마지막 메시지 idx 조회
        chatMessageRepository.findTopByChatRoomsIdxOrderByCreatedAtDesc(roomIdx)
                .ifPresent(msg -> participant.updateLastReadMessageId(msg.getIdx()));
    }
}