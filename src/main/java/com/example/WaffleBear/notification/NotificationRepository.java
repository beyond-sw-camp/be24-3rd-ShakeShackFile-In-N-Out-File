package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    // isActive = true인 구독만 조회 (로그인 상태)
    List<NotificationEntity> findByUserIdxAndIsActiveTrue(Long userIdx);
    List<NotificationEntity> findAllByEndpoint(String endpoint);
    Optional<NotificationEntity> findByEndpoint(String endpoint);
    // 로그아웃 시 해당 유저의 모든 구독 비활성화용
    List<NotificationEntity> findAllByUserIdx(Long userIdx);
}
