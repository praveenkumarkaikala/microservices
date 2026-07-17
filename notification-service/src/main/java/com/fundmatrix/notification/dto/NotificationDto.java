package com.fundmatrix.notification.dto;

import com.fundmatrix.notification.domain.enums.NotificationCategory;
import com.fundmatrix.notification.domain.enums.NotificationStatus;

import java.time.Instant;

public record NotificationDto(
        Long id,
        Long userId,
        String message,
        NotificationCategory category,
        NotificationStatus status,
        Instant createdDate
) {
}
