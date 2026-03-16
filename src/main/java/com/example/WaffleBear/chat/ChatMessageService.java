package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatMessagesDto;
import com.example.WaffleBear.chat.model.entity.ChatMessages;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;


    @Transactional(readOnly = true)
    public ChatMessagesDto.PageRes getMessageList(Long roomIdx, Long userIdx, int page, int size) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

        boolean isParticipant = participantsRepository.existsByChatRoomsIdxAndUsersIdx(room.getIdx(), userIdx);

        if (!isParticipant) {
            throw new RuntimeException("해당 채팅방에 접근 권한이 없습니다.");
        }
        // Pageable 임포트 오류 해결 및 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("sendTime").descending());
        Page<ChatMessages> result = chatMessageRepository.findAllByChatRooms(room, pageable);

        return ChatMessagesDto.PageRes.from(result);
    }

    @Transactional
    public ChatMessagesDto.ListRes saveMessage(Long roomIdx, ChatMessagesDto.Send req, Long userIdx) {
        ChatRooms room = chatRoomRepository.findById(roomIdx)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 메시지 생성 및 저장
        ChatMessages message = chatMessageRepository.save(req.toEntity(room, user));

        // ChatRooms 정보 업데이트
        room.builder()
                .lastMessage(message.getContents())
                .lastMessageTime(message.getSendTime())
                .build();

        // 전송용 응답 DTO 반환
        return ChatMessagesDto.ListRes.from(message);
    }
}