package com.fundmatrix.navaccounting.service;

import com.fundmatrix.navaccounting.client.NotificationClient;
import com.fundmatrix.navaccounting.client.NotificationRequest;
import com.fundmatrix.navaccounting.domain.enums.NotificationCategory;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around {@link NotificationClient}. The monolith's `notificationService.notify(User,
 * category, message)` took a full User entity; since this service doesn't own User data, callers
 * now pass the plain investor id (resolved from HoldingDto.investorId() upstream).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void notify(Long userId, NotificationCategory category, String message) {
        if (userId == null) {
            return;
        }
        try {
            notificationClient.notify(new NotificationRequest(userId, category.name(), message));
        } catch (FeignException ex) {
            // Best-effort: a notification-service outage must not block NAV publication/dividend processing.
            log.warn("Failed to notify user {} [{}]: {}", userId, category, ex.getMessage());
        }
    }
}
