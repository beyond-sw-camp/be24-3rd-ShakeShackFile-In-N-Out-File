package com.example.WaffleBear.config.interceptor;


import com.example.WaffleBear.chat.ChatRoomService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckRoomAuthInterceptor implements HandlerInterceptor {
    private final ChatRoomService chatroomService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // PathVariable에서 roomId 추출 (예: /chat/room/{roomId})
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String roomIdStr = pathVariables != null ? pathVariables.get("id") : null;

        if (roomIdStr == null) {
            // 초대 로직 등에서 body에 roomId가 있는 경우 처리 (단, 여기서는 PathVariable 위주로 예시)
            return true;
        }

        Long roomId = Long.parseLong(roomIdStr);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof AuthUserDetails user) {
            if (chatroomService.isMember(roomId, user.getIdx())) {
                return true;
            }
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Not a member of this chat room.");
        return false;
    }
}
