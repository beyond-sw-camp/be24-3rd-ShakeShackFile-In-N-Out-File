package com.example.WaffleBear.chat;

import com.example.WaffleBear.chat.model.entity.ChatParticipants;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantsRepository extends JpaRepository<ChatParticipants,Long> {
}
