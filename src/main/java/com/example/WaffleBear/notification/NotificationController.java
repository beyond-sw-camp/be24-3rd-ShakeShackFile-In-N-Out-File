package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.jose4j.lang.JoseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    // ── 기존 엔드포인트 (변경 없음) ──────────────────────────────────────────

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Subscribe dto) {
        notificationService.subscribe(dto, user.getIdx());
        return ResponseEntity.ok("성공");
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(
            @RequestBody NotificationDto.Send dto
    ) throws JoseException, GeneralSecurityException, IOException, ExecutionException, InterruptedException {
        notificationService.send(dto);
        return ResponseEntity.ok("성공");
    }

    // ── ★ 추가: 인박스 알림 목록 조회 ────────────────────────────────────────

    /**
     * 로그인 유저의 알림 목록 반환
     * 프론트: GET /notification/list
     * 응답: { result: { body: [ { idx, uuid, type, title, message, read, createdAt } ] } }
     */
    @GetMapping("/list")
    public ResponseEntity<?> list(@AuthenticationPrincipal AuthUserDetails user) {
        List<NotificationDto.InboxItem> items =
                notificationService.getInboxNotifications(user.getIdx());
        return ResponseEntity.ok(
                Map.of("result", Map.of("body", items))
        );
    }
}