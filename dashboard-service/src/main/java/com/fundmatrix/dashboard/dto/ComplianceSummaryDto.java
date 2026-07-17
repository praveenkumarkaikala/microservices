package com.fundmatrix.dashboard.dto;

import java.util.List;

/** KYC compliance + flagged-transaction overview for the Compliance Portal. */
public record ComplianceSummaryDto(
        long compliantCount,
        long nonCompliantCount,
        long pendingCount,
        long expiredCount,
        long flaggedTransactionCount,
        List<TransactionDto> flaggedTransactions
) {
}
