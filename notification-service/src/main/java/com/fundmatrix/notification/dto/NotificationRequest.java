package com.fundmatrix.notification.dto;

/**
 * Internal, service-to-service contract (FEIGN_CONTRACTS.md - notification-service section).
 * category is the NotificationCategory enum name as a string, e.g. "TRANSACTION", "DIVIDEND",
 * "COMMISSION", "NAV", "KYC".
 */
public record NotificationRequest(Long userId, String category, String message) {
}
