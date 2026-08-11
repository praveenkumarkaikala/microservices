package com.fundmatrix.compliance.client;


public record NotificationRequest(Long userId, String category, String message) {
}
