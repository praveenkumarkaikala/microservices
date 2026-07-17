package com.fundmatrix.compliance.client;

/** category is the NotificationCategory enum name as a string, e.g. "KYC". */
public record NotificationRequest(Long userId, String category, String message) {
}
