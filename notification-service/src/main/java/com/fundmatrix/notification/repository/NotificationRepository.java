package com.fundmatrix.notification.repository;

import com.fundmatrix.notification.domain.Notification;
import com.fundmatrix.notification.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedDateDesc(Long userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedDateDesc(Long userId, NotificationStatus status);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);
}
