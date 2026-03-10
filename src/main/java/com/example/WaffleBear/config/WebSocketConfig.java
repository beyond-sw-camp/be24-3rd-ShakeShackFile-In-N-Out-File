package com.example.WaffleBear.Config;


import com.example.WaffleBear.Config.interceptor.CheckRoomAuthInterceptor;
import com.example.WaffleBear.Config.interceptor.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketHandler webSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final CheckRoomAuthInterceptor checkRoomAuthInterceptor;
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
//                  .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
        // 웹 브라우저에서 WS 프로토콜을 지원하지 않는 경우 WS 대신에 HTTP로 통신할 수 있게 해주는 라이브러리를 사용할 때 설정
        //.withSockJS();
    }

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        registration.interceptors(checkRoomAuthInterceptor);
//    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 구독자가 메시지를 받을 경로의 시작 부분
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트가 메시지를 보낼 때 사용할 주소의 시작 부분
        registry.setUserDestinationPrefix("/user"); // 특정 사용자에게 메시지를 보낼 때 사용할 주소의 시작 부분
    }
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(webSocketHandler, "/ws")
//                .setAllowedOrigins("*")
//                .addInterceptors(jwtHandshakeInterceptor);
//    }

}