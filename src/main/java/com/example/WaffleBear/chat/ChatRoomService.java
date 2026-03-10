package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.dto.ChatParticipantsDto;
import com.example.WaffleBear.chat.model.dto.ChatRoomsDto;
import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import com.example.WaffleBear.chat.model.entity.ChatRooms;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.user.model.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ParticipantsRepository participantsRepository;

        // 1. 방 생성 (내부에 초대 로직 포함)
        @Transactional
        public Long createChatRoom(ChatRoomsDto.ChatRoomsReq dto, Long myIdx) {
            // 방 엔티티 생성 및 저장
            ChatRooms room = chatRoomRepository.save(dto.toEntity());

            // 초대할 유저 ID 목록 (나 포함)
            Set<Long> allUserIdx = new HashSet<>(dto.getParticipantsIdx());
            allUserIdx.add(myIdx);

            // 공통 초대 메서드 호출
            this.addParticipantsToRoom(room, allUserIdx);

            return room.getIdx();
        }

        // 2. 기존 방에 추가 초대 (초대하기 기능)
        @Transactional
        public void inviteUsers(Long roomId, List<Long> userIdx) {
            ChatRooms room = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

            // 공통 초대 메서드 호출
            this.addParticipantsToRoom(room, new HashSet<>(userIdx));
        }

        // [공통 로직] 실제 참가자 테이블에 저장하는 메서드
        private void addParticipantsToRoom(ChatRooms room, Set<Long> userIdx) {
            List<User> users = userRepository.findAllById(userIdx);

            List<ChatParticipants> participants = users.stream()
                    .map(user -> ChatParticipantsDto.Create.toEntity(room, user))
                    .collect(Collectors.toList());

            participantsRepository.saveAll(participants);
        }

    public ChatRoomsDto.PageRes list(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        // 페이징 처리 ⭕, 페이지 번호가 필요하다 => Page 반환
        // 페이징 처리 ⭕, 페이지 번호가 필요없다. => Slice 반환
        Page<ChatRooms> result = chatRoomRepository.findAll(pageRequest);

        return ChatRoomsDto.PageRes.from(result);
    }
    public void delete(Long idx) {
        chatRoomRepository.deleteById(idx);
    }

}
