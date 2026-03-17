package com.example.WaffleBear.notification;

import com.example.WaffleBear.notification.model.NotificationListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationListRepository extends JpaRepository<NotificationListEntity, Long> {

    /** 수신자 userIdx 기준으로 최신순 조회 */
    List<NotificationListEntity> findByReceiverUserIdxOrderByCreatedAtDesc(Long receiverUserIdx);
}
