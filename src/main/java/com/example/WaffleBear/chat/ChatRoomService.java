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
import org.springframework.data.domain.Sort;
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

    public ChatRoomsDto.PageRes list(int page, int size, Long userIdx) {
        PageRequest pageRequest = PageRequest.of(page, size);

        // 1. 내가 참여자로 등록된 데이터들을 가져옵니다.
        Page<ChatParticipants> result = participantsRepository.findAllByUsersIdx(userIdx, pageRequest);

        return ChatRoomsDto.PageRes.from(result);
    }
    @Transactional
    public void exit(Long roomIdx, Long userIdx) {
        // 1. 해당 방에서 내가 참여자인지 확인하고 삭제
        participantsRepository.deleteByChatRoomsIdxAndUsersIdx(roomIdx, userIdx);

        // 2. (선택 사항) 만약 방에 남은 참여자가 아무도 없다면 방 자체를 삭제
        if (!participantsRepository.existsByChatRoomsIdx(roomIdx)) {
            chatRoomRepository.deleteById(roomIdx);
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
}
