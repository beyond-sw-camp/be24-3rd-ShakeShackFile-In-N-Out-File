package com.example.WaffleBear.Config.interceptor;


import com.example.WaffleBear.user.model.AuthUserDetails;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CheckRoomAuthInterceptor implements ChannelInterceptor {
    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String path = accessor.getDestination();

            Map<String, Object> attributes =  accessor.getSessionAttributes();

            Authentication authentication = (Authentication)attributes.get("user");
            AuthUserDetails user = (AuthUserDetails)authentication.getPrincipal();

            if(path.startsWith("/topic/")) {
                String roomIdx = path.substring("/topic/".length());

                List<Long> roomIdxList = List.of(3L, 4L);
                Long requestRoomIdx = Long.parseLong(roomIdx);
                if(!roomIdxList.contains(requestRoomIdx)) {
                    throw new IllegalArgumentException();
                }
            }
        }

        return message;
    }
}