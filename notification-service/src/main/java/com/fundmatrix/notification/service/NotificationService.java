package com.fundmatrix.notification.service;

import com.fundmatrix.notification.common.exception.ResourceNotFoundException;
import com.fundmatrix.notification.domain.Notification;
import com.fundmatrix.notification.domain.enums.NotificationCategory;
import com.fundmatrix.notification.domain.enums.NotificationStatus;
import com.fundmatrix.notification.dto.NotificationDto;
import com.fundmatrix.notification.dto.NotificationRequest;
import com.fundmatrix.notification.repository.NotificationRepository;
import com.fundmatrix.notification.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUser;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserService currentUser) {
        this.notificationRepository = notificationRepository;
        this.currentUser = currentUser;
    }

    /**
     * Replaces the monolith's {@code notify(User user, NotificationCategory category, String message)}.
     * There's no User object in this service, so it now takes the plain userId.
     */
    @Transactional
    public void notify(Long userId, NotificationCategory category, String message) {
        if (userId == null) {
            return;
        }
        Notification n = Notification.builder()
                .userId(userId)
                .category(category)
                .message(message)
                .status(NotificationStatus.UNREAD)
                .createdDate(Instant.now())
                .build();
        notificationRepository.save(n);
    }

    /**
     * Backing implementation for the internal POST /notifications contract
     * (FEIGN_CONTRACTS.md - notification-service section), consumed by folio-transaction,
     * distributor-commission, nav-accounting and compliance-kyc services.
     */
    @Transactional
    public void notify(NotificationRequest request) {
        notify(request.userId(), NotificationCategory.valueOf(request.category()), request.message());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> myNotifications() {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(currentUser.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long myUnreadCount() {
        return notificationRepository.countByUserIdAndStatus(currentUser.getId(), NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationDto updateStatus(Long id, NotificationStatus status) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        if (!n.getUserId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        n.setStatus(status);
        return toDto(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead() {
        var list = notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(
                currentUser.getId(), NotificationStatus.UNREAD);
        list.forEach(n -> n.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(list);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getUserId(), n.getMessage(), n.getCategory(),
                n.getStatus(), n.getCreatedDate());
    }
}
