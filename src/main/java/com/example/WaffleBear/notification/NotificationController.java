package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jose4j.lang.JoseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
@Tag(name = "Notification", description = "Web push subscription and inbox APIs")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe push", description = "Registers a web push subscription for the current user.")
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Subscribe dto) {
        notificationService.subscribe(dto, user.getIdx());
        return ResponseEntity.ok("구독 성공");
    }

    // 로그아웃 시 호출 → isActive = false
    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe push", description = "Disables the current user's push subscription.")
    public ResponseEntity<?> unsubscribe(
            @AuthenticationPrincipal AuthUserDetails user) {
        notificationService.unsubscribe(user.getIdx());
        return ResponseEntity.ok("구독 해지 성공");
    }

    @PostMapping("/send")
    @Operation(summary = "Send notification", description = "Sends a push notification payload.")
    public ResponseEntity<?> send(
            @RequestBody NotificationDto.Send dto
    ) throws JoseException, GeneralSecurityException, IOException, ExecutionException, InterruptedException {
        notificationService.send(dto);
        return ResponseEntity.ok("전송 성공");
    }

    @GetMapping("/list")
    @Operation(summary = "List inbox notifications", description = "Returns the current user's notification inbox.")
    public ResponseEntity<?> list(@AuthenticationPrincipal AuthUserDetails user) {
        List<NotificationDto.InboxItem> items =
                notificationService.getInboxNotifications(user.getIdx());
        return ResponseEntity.ok(
                Map.of("result", Map.of("body", items))
        );
    }

    @PatchMapping("/read")
    @Operation(summary = "Mark notification as read", description = "Marks a notification target as read.")
    public ResponseEntity<?> read(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Target dto
    ) {
        notificationService.markAsRead(user.getIdx(), dto);
        return ResponseEntity.ok("읽음 처리 성공");
    }

    @DeleteMapping
    @Operation(summary = "Delete notification", description = "Deletes a notification from the inbox.")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Target dto
    ) {
        notificationService.deleteNotification(user.getIdx(), dto);
        return ResponseEntity.ok("삭제 성공");
    }
}
