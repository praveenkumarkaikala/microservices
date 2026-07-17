package com.fundmatrix.navaccounting.client;

import java.math.BigDecimal;

/** Mirrors folio-transaction-service's CreditUnitsRequest (FEIGN_CONTRACTS.md). */
public record CreditUnitsRequest(
        Long folioId,
        Long schemeId,
        Long optionId,
        BigDecimal units,
        BigDecimal investedAmount,
        BigDecimal navValue
) {
}
