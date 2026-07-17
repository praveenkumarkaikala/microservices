package com.fundmatrix.dashboard.client;

/** Own lean copy of distributor-commission-service's DistributorDto - only the fields dashboard-service needs. */
public record DistributorDto(Long id, String name, String arnNumber, String status, Long userId) {
}
