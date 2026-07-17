package com.fundmatrix.dashboard.client;

import java.math.BigDecimal;

/** Own lean copy of distributor-commission-service's TrailCommissionDto - only the fields dashboard-service needs. */
public record TrailCommissionDto(Long id, Long distributorId, BigDecimal commissionAmount, String status) {
}
