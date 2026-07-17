package com.fundmatrix.transaction.dto;

import java.math.BigDecimal;

/** Producer-side internal DTO for /holdings/** endpoints, consumed by other services. */
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
