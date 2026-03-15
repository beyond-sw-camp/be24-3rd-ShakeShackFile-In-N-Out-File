package com.example.WaffleBear.notification;


import com.example.WaffleBear.notification.model.NotificationDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.jose4j.lang.JoseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;
    
    @PostMapping("/subscribe")
    public ResponseEntity subscribe(@AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Subscribe dto) {
        notificationService.subscribe(dto, user.getIdx());
        return ResponseEntity.ok("성공");
    }

    @PostMapping("/send")
    public ResponseEntity send(@RequestBody NotificationDto.Send dto) throws JoseException, GeneralSecurityException, IOException, ExecutionException, InterruptedException {
        notificationService.send(dto);
        return ResponseEntity.ok("성공");
    }
}
