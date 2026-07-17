package com.fundmatrix.foliokyc.dto;

import java.math.BigDecimal;

public record CreditUnitsRequest(Long folioId, Long schemeId, Long optionId,
                                 BigDecimal units, BigDecimal investedAmount,
                                 BigDecimal navValue) {
}
