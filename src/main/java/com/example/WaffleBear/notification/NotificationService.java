package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationDto;
import com.example.WaffleBear.notification.model.NotificationEntity;
import com.example.WaffleBear.notification.model.NotificationListEntity;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationListRepository nlr; // ★ 추가
    private final PushService pushService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationListRepository nlr // ★ 추가
    ) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        this.notificationRepository = notificationRepository;
        this.nlr = nlr; // ★ 추가

        if (Security.getProperty(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.pushService = new PushService();
        this.pushService.setPublicKey("BLHgfPga02L2u89uc4xjhbUFTy_U04rQCjGq7o24oxtqfVmAPHTxOmp6xndSHZtGQpmt7gqTFdMXco2gRNP7_p8");
        this.pushService.setPrivateKey("비밀임");
        this.pushService.setSubject("우리 사이트이다");
    }

    // ── 기존 메서드 (변경 없음) ───────────────────────────────────────────────

    public void subscribe(NotificationDto.Subscribe dto, Long userIdx) {
        notificationRepository.findByEndpoint(dto.getEndpoint())
                .ifPresentOrElse(
                        existing -> {
                            NotificationEntity updated = NotificationEntity.builder()
                                    .idx(existing.getIdx())
                                    .userIdx(userIdx)
                                    .endpoint(existing.getEndpoint())
                                    .p256dh(dto.getKeys().get("p256dh"))
                                    .auth(dto.getKeys().get("auth"))
                                    .build();
                            notificationRepository.save(updated);
                        },
                        () -> notificationRepository.save(dto.toEntity(userIdx))
                );
    }

    public void send(NotificationDto.Send dto) throws GeneralSecurityException, JoseException, IOException, ExecutionException, InterruptedException {
        NotificationEntity entity = notificationRepository.findById(dto.getIdx()).orElseThrow();
        Subscription.Keys keys = new Subscription.Keys(entity.getP256dh(), entity.getAuth());
        Subscription subscription = new Subscription(entity.getEndpoint(), keys);
        Notification notification = new Notification(subscription, NotificationDto.Payload.from(dto).toString());
        pushService.send(notification);
    }

    public void sendToUser(Long userIdx, String title, String message, Long roomIdx, Long unreadCount) {
        notificationRepository.findByUserIdx(userIdx).forEach(entity -> {
            try {
                Subscription.Keys keys = new Subscription.Keys(entity.getP256dh(), entity.getAuth());
                Subscription subscription = new Subscription(entity.getEndpoint(), keys);
                Notification notification = new Notification(
                        subscription,
                        NotificationDto.Payload.create(title, message, roomIdx, unreadCount).toString()
                );
                pushService.send(notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ── ★ 추가: 워크스페이스 초대 알림 ────────────────────────────────────────

    /**
     * 초대 이메일 발송 후 호출.
     * 1) inbox_notification 테이블에 알림 레코드 저장
     * 2) 해당 유저의 등록된 모든 기기에 웹 푸시 발송
     *
     * @param receiverUserIdx 초대받는 유저의 idx (workspace 서비스에서 email → idx 변환 후 전달)
     * @param uuid            초대 토큰 (수락/거절 시 verifyEmail 에 넘길 값)
     * @param workspaceName   알림 메시지에 표시할 워크스페이스 이름
     */
    public void sendWorkspaceInviteNotification(Long receiverUserIdx, String uuid, String workspaceName) {
        // 1. 인박스 알림 저장
        NotificationListEntity inbox = NotificationListEntity.builder()
                .receiverUserIdx(receiverUserIdx)
                .uuid(uuid)
                .type("invite")
                .title("워크스페이스 초대")
                .message("[" + workspaceName + "] 워크스페이스에 초대되었습니다.")
                .build();
        nlr.save(inbox);

        // 2. 웹 푸시 발송
        NotificationDto.Payload payload = NotificationDto.Payload.createInvite(
                "워크스페이스 초대",
                "[" + workspaceName + "] 워크스페이스에 초대되었습니다.",
                uuid
        );

        notificationRepository.findByUserIdx(receiverUserIdx).forEach(entity -> {
            try {
                Subscription.Keys keys = new Subscription.Keys(entity.getP256dh(), entity.getAuth());
                Subscription subscription = new Subscription(entity.getEndpoint(), keys);
                Notification notification = new Notification(subscription, payload.toString());
                pushService.send(notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 로그인 유저의 인박스 알림 목록 반환 (최신순)
     *
     * @param userIdx 로그인 유저 idx
     */
    public List<NotificationDto.InboxItem> getInboxNotifications(Long userIdx) {
        return nlr
                .findByReceiverUserIdxOrderByCreatedAtDesc(userIdx)
                .stream()
                .map(NotificationDto.InboxItem::from)
                .collect(Collectors.toList());
    }
}