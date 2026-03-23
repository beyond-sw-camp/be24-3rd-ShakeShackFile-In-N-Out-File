package com.example.WaffleBear.config.websocket;

import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = new HashSet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("클라이언트가 웹 소켓 서버 접속함");
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("메시지 전송 받음 : {}", message.toString());
        Authentication auth = (Authentication) session.getAttributes().get("user");
        AuthUserDetails user = (AuthUserDetails) auth.getPrincipal();

        for (WebSocketSession to : sessions) {
            if (to.equals(session)) {
                continue;
            }
            to.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("클라이언트가 웹 소켓 서버 열결 해지함");
    }
}