package com.fundmatrix.notification.controller;

import com.fundmatrix.notification.domain.enums.NotificationStatus;
import com.fundmatrix.notification.dto.MessageResponse;
import com.fundmatrix.notification.dto.NotificationDto;
import com.fundmatrix.notification.dto.NotificationRequest;
import com.fundmatrix.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the current user")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/mine")
    public List<NotificationDto> mine() {
        return notificationService.myNotifications();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.myUnreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id) {
        return notificationService.updateStatus(id, NotificationStatus.READ);
    }

    @PatchMapping("/{id}/dismiss")
    public NotificationDto dismiss(@PathVariable Long id) {
        return notificationService.updateStatus(id, NotificationStatus.DISMISSED);
    }

    @PostMapping("/read-all")
    public MessageResponse markAllRead() {
        notificationService.markAllRead();
        return new MessageResponse("All notifications marked as read");
    }

    /**
     * Internal, service-to-service contract (FEIGN_CONTRACTS.md - notification-service
     * section). Consumed by folio-transaction, distributor-commission, nav-accounting and
     * compliance-kyc services via their NotificationClient Feign client.
     */
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody NotificationRequest request) {
        notificationService.notify(request);
        return ResponseEntity.ok().build();
    }
}
