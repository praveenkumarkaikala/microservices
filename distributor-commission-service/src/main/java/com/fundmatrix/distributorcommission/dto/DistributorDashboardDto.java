package com.fundmatrix.distributorcommission.dto;

import java.math.BigDecimal;

/** Aggregated AUM / commission snapshot for the Distributor Console. */
public record DistributorDashboardDto(
        Long distributorId,
        String distributorName,
        String arnNumber,
        int clientFolioCount,
        BigDecimal aumManaged,
        BigDecimal commissionEarnedToDate,
        BigDecimal commissionPending
) {
}
