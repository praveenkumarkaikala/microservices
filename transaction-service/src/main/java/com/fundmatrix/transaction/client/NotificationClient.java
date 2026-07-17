package com.fundmatrix.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/api")
public interface NotificationClient {

    record NotificationRequest(Long userId, String category, String message) {
    }

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest request);
}
