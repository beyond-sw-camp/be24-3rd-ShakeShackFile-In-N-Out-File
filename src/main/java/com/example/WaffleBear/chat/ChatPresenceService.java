package com.example.WaffleBear.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    private String key(Long roomId, Long userId) {
        return "chat:presence:room:" + roomId + ":user:" + userId;
    }

    public void enter(Long roomId, Long userId) {
        redisTemplate.opsForValue().set(key(roomId, userId), "1", TTL);
    }

    public void refresh(Long roomId, Long userId) {
        redisTemplate.opsForValue().set(key(roomId, userId), "1", TTL);
    }

    public void leave(Long roomId, Long userId) {
        redisTemplate.delete(key(roomId, userId));
    }

    public boolean isActiveInRoom(Long roomId, Long userId) {
        Boolean exists = redisTemplate.hasKey(key(roomId, userId));
        return Boolean.TRUE.equals(exists);
    }
}
