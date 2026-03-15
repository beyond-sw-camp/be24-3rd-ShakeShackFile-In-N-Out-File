package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUserIdx(Long userIdx);
    Optional<NotificationEntity> findByEndpoint(String endpoint); // 엔드포인트 중복 방지용
}
