package com.example.WaffleBear.legup.openhexagon;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class OpenHexagonDisconnectListener {

    private final OpenHexagonService openHexagonService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        OpenHexagonDto.LobbySnapshot snapshot = openHexagonService.leave(event.getSessionId());
        messagingTemplate.convertAndSend("/sub/game/openhexagon/lobby", snapshot);
    }
}
