package com.fundmatrix.navaccounting.client;

import java.math.BigDecimal;

/** Mirrors folio-transaction-service's HoldingDto (FEIGN_CONTRACTS.md). */
public record HoldingDto(
        Long id,
        Long folioId,
        Long schemeId,
        Long optionId,
        BigDecimal unitsHeld,
        BigDecimal averageCostNav,
        BigDecimal currentValue,
        BigDecimal unrealisedGainLoss,
        Long investorId
) {
}
