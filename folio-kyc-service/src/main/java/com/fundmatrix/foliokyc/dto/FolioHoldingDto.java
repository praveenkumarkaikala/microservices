package com.fundmatrix.foliokyc.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FolioHoldingDto(
        Long id,
        Long folioId,
        String folioNumber,
        Long schemeId,
        String schemeName,
        Long optionId,
        String optionType,
        BigDecimal unitsHeld,
        BigDecimal averageCostNav,
        BigDecimal latestNav,
        BigDecimal currentValue,
        BigDecimal unrealisedGainLoss,
        Instant lastUpdated
) {
}
