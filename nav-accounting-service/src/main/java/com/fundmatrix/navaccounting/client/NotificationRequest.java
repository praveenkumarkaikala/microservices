package com.fundmatrix.navaccounting.client;

/** category is the NotificationCategory enum name as a string, e.g. "NAV", "DIVIDEND". */
public record NotificationRequest(Long userId, String category, String message) {
}
