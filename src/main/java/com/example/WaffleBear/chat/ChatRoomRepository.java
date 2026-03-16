package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatRooms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRooms,Long> {
}
