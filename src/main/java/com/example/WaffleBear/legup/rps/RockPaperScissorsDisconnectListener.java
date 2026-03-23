package com.example.WaffleBear.legup.rps;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class RockPaperScissorsDisconnectListener {

    private final RockPaperScissorsService rockPaperScissorsService;
    private final RockPaperScissorsSocketController rockPaperScissorsSocketController;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        rockPaperScissorsSocketController.broadcast(rockPaperScissorsService.leave(event.getSessionId()));
    }
}
