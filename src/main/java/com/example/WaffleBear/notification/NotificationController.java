package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "알림 (Notification)", description = "웹 푸시 알림 구독, 발송, 수신함 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "푸시 알림 구독", description = "웹 푸시 알림을 구독합니다. 브라우저의 Push API 구독 정보를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Subscribe dto) {
        notificationService.subscribe(dto, user.getIdx());
        return ResponseEntity.ok("구독 성공");
    }

    // 로그아웃 시 호출 → isActive = false
    @Operation(summary = "푸시 알림 구독 해지", description = "웹 푸시 알림 구독을 해지합니다. 로그아웃 시 호출됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 해지 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(
            @AuthenticationPrincipal AuthUserDetails user) {
        notificationService.unsubscribe(user.getIdx());
        return ResponseEntity.ok("구독 해지 성공");
    }

    @Operation(summary = "알림 발송", description = "특정 사용자에게 푸시 알림을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "대상 사용자의 구독 정보를 찾을 수 없음")
    })
    @PostMapping("/send")
    public ResponseEntity<?> send(
            @RequestBody NotificationDto.Send dto
    ) throws JoseException, GeneralSecurityException, IOException, ExecutionException, InterruptedException {
        notificationService.send(dto);
        return ResponseEntity.ok("전송 성공");
    }

    @Operation(summary = "알림 수신함 조회", description = "현재 사용자의 알림 수신함 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/list")
    public ResponseEntity<?> list(@AuthenticationPrincipal AuthUserDetails user) {
        List<NotificationDto.InboxItem> items =
                notificationService.getInboxNotifications(user.getIdx());
        return ResponseEntity.ok(
                Map.of("result", Map.of("body", items))
        );
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/read")
    public ResponseEntity<?> read(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Target dto
    ) {
        notificationService.markAsRead(user.getIdx(), dto);
        return ResponseEntity.ok("읽음 처리 성공");
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.Target dto
    ) {
        notificationService.deleteNotification(user.getIdx(), dto);
        return ResponseEntity.ok("삭제 성공");
    }
}