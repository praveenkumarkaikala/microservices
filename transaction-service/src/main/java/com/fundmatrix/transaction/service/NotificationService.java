package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.NotificationClient;
import com.fundmatrix.transaction.client.NotificationClient.NotificationRequest;
import com.fundmatrix.transaction.domain.enums.NotificationCategory;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over notification-service's Feign client. The monolith's notify(User, ...)
 * took a full User entity; since User now lives in auth-user-service, call sites pass
 * the plain investor/distributor user id instead.
 */
@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void notify(Long userId, NotificationCategory category, String message) {
        if (userId == null) {
            return;
        }
        notificationClient.notify(new NotificationRequest(userId, category.name(), message));
    }
}
