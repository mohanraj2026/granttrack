package com.granttrack.notification.repository;

import com.granttrack.notification.entity.Notification;
import com.granttrack.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    long countByUserIdAndStatus(Long userId, NotificationStatus status);
}
