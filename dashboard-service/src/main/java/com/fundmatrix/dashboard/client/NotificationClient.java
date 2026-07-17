package com.fundmatrix.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/** Read-only consumer of notification-service (owns Notification). */
@FeignClient(name = "notification-service", path = "/api")
public interface NotificationClient {

    /**
     * Existing monolith-derived route (NotificationController.unreadCount) - resolves to the
     * CURRENT authenticated user (JWT forwarded by FeignConfig), exactly like the monolith's
     * in-process NotificationRepository.countByUser_IdAndStatus(investorId, UNREAD) call.
     */
    @GetMapping("/notifications/unread-count")
    Map<String, Long> unreadCount();
}
