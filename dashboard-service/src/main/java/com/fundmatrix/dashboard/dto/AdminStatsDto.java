package com.fundmatrix.dashboard.dto;

import java.math.BigDecimal;

/** High-level platform statistics for the Admin Console landing page. */
public record AdminStatsDto(
        long totalUsers,
        long totalSchemes,
        long activeSchemes,
        long totalFolios,
        long totalDistributors,
        long pendingTransactions,
        BigDecimal totalAum
) { }
