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
    private final NotificationListRepository nlr;
    private final PushService pushService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationListRepository nlr
    ) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        this.notificationRepository = notificationRepository;
        this.nlr = nlr;

        if (Security.getProperty(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.pushService = new PushService();
        this.pushService.setPublicKey("BLHgfPga02L2u89uc4xjhbUFTy_U04rQCjGq7o24oxtqfVmAPHTxOmp6xndSHZtGQpmt7gqTFdMXco2gRNP7_p8");
        this.pushService.setPrivateKey("pWhOI-mTyOyx5hogOmKRiYHDCtm_IMpnz1lzWNdMfKU");
        this.pushService.setSubject("mailto:no-reply@fileinnout.local");
    }

    public void subscribe(NotificationDto.Subscribe dto, Long userIdx) {
        notificationRepository.findByEndpoint(dto.endpoint())
                .ifPresentOrElse(
                        existing -> {
                            NotificationEntity updated = NotificationEntity.builder()
                                    .idx(existing.getIdx())
                                    .userIdx(userIdx)
                                    .endpoint(existing.getEndpoint())
                                    .p256dh(dto.keys() != null ? dto.keys().get("p256dh") : null)
                                    .auth(dto.keys() != null ? dto.keys().get("auth") : null)
                                    .build();
                            notificationRepository.save(updated);
                        },
                        () -> notificationRepository.save(dto.toEntity(userIdx))
                );
    }

    public void send(NotificationDto.Send dto) throws GeneralSecurityException, JoseException, IOException, ExecutionException, InterruptedException {
        NotificationEntity entity = notificationRepository.findById(dto.idx()).orElseThrow();
        Subscription.Keys keys = new Subscription.Keys(entity.getP256dh(), entity.getAuth());
        Subscription subscription = new Subscription(entity.getEndpoint(), keys);
        Notification notification = new Notification(subscription, NotificationDto.Payload.from(dto).toString());
        pushService.send(notification);
    }

    public void sendToUser(Long userIdx, String title, String message, Long roomIdx, Long unreadCount) {
        sendPayloadToUser(userIdx, NotificationDto.Payload.create(title, message, roomIdx, unreadCount));
    }

    public void sendWorkspaceInviteNotification(Long receiverUserIdx, String uuid, String workspaceName) {
        NotificationListEntity inbox = NotificationListEntity.builder()
                .receiverUserIdx(receiverUserIdx)
                .uuid(uuid)
                .type("invite")
                .title("워크스페이스 초대")
                .message("[" + workspaceName + "] 워크스페이스에 초대되었습니다.")
                .build();

        inbox = nlr.save(inbox);
        sendPayloadToUser(receiverUserIdx, NotificationDto.Payload.fromInbox(inbox));
    }

    public void sendGeneralNotification(Long receiverUserIdx, String title, String message) {
        NotificationListEntity inbox = NotificationListEntity.builder()
                .receiverUserIdx(receiverUserIdx)
                .type("general")
                .title(title)
                .message(message)
                .build();

        inbox = nlr.save(inbox);
        sendPayloadToUser(receiverUserIdx, NotificationDto.Payload.fromInbox(inbox));
    }

    public List<NotificationDto.InboxItem> getInboxNotifications(Long userIdx) {
        return nlr.findByReceiverUserIdxOrderByCreatedAtDesc(userIdx)
                .stream()
                .map(NotificationDto.InboxItem::from)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long userIdx, NotificationDto.Target dto) {
        NotificationListEntity entity = findInboxTarget(userIdx, dto);
        entity.markAsRead();
        nlr.save(entity);
    }

    public void deleteNotification(Long userIdx, NotificationDto.Target dto) {
        NotificationListEntity entity = findInboxTarget(userIdx, dto);
        nlr.delete(entity);
    }

    private NotificationListEntity findInboxTarget(Long userIdx, NotificationDto.Target dto) {
        if (dto.id() != null) {
            return nlr.findByIdxAndReceiverUserIdx(dto.id(), userIdx)
                    .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        }

        if (dto.uuid() != null && !dto.uuid().isBlank()) {
            return nlr.findByUuidAndReceiverUserIdx(dto.uuid(), userIdx)
                    .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        }

        throw new IllegalArgumentException("id 또는 uuid가 필요합니다.");
    }

    private void sendPayloadToUser(Long receiverUserIdx, NotificationDto.Payload payload) {
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
}
