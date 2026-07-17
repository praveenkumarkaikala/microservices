package com.fundmatrix.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated portfolio snapshot for the Investor Portal dashboard. */
public record InvestorDashboardDto(
        int folioCount,
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal unrealisedGainLoss,
        int activeSipCount,
        long unreadNotifications,
        List<FolioHoldingDto> holdings
) {
}
