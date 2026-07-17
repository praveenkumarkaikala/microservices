package com.fundmatrix.navaccounting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/api")
public interface NotificationClient {

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest request);
}
