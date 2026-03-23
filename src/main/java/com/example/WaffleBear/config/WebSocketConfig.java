package com.example.WaffleBear.config;


import com.example.WaffleBear.chat.ChatRoomService;
import com.example.WaffleBear.config.interceptor.CheckRoomAuthInterceptor;
import com.example.WaffleBear.config.interceptor.JwtHandshakeInterceptor;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.utils.JwtUtil;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final ChatRoomService chatRoomService;
    private final JwtUtil jwtUtil;
    private final UserPostRepository userPostRepository;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins(frontendUrl)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // CONNECT 시 토큰 파싱해서 유저 등록
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.replace("Bearer ", "");
                        try {
                            Long idx = jwtUtil.getUserIdx(token);
                            String email = jwtUtil.getEmail(token);
                            String role = jwtUtil.getRole(token);

                            AuthUserDetails user = AuthUserDetails.builder()
                                    .idx(idx).email(email).role(role).build();

                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                    user, null, List.of(new SimpleGrantedAuthority(role)));

                            accessor.setUser(auth); // ← 핵심
                        } catch (Exception e) {
                            throw new RuntimeException("유효하지 않은 토큰입니다.");
                        }
                    }
                }

                // SUBSCRIBE 시 권한 확인
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    Authentication auth = (Authentication) accessor.getUser();
                    if (destination != null && destination.startsWith("/sub/chat/room/")) {
                        Long roomId = Long.parseLong(destination.replace("/sub/chat/room/", ""));

                        if (auth != null && auth.getPrincipal() instanceof AuthUserDetails user) {
                            if (!chatRoomService.isMember(roomId, user.getIdx())) {
                                throw new RuntimeException("채팅방 접근 권한이 없습니다.");
                            }
                        } else {
                            throw new RuntimeException("인증되지 않은 사용자입니다.");
                        }
                    }

                    if (destination != null && destination.startsWith("/sub/workspace/assets/")) {
                        Long workspaceId = Long.parseLong(destination.replace("/sub/workspace/assets/", ""));

                        if (auth != null && auth.getPrincipal() instanceof AuthUserDetails user) {
                            if (userPostRepository.findByUser_IdxAndWorkspace_Idx(user.getIdx(), workspaceId).isEmpty()) {
                                throw new RuntimeException("워크스페이스 접근 권한이 없습니다.");
                            }
                        } else {
                            throw new RuntimeException("인증되지 않은 사용자입니다.");
                        }
                    }
                }

                return message;
            }
        });

    }
}
