package com.fundmatrix.distributorcommission.client;

/** category is the NotificationCategory enum name as a string, e.g. "COMMISSION". */
public record NotificationRequest(Long userId, String category, String message) {
}
