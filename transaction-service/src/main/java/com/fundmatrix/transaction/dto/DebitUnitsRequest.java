package com.fundmatrix.transaction.dto;

import java.math.BigDecimal;

public record DebitUnitsRequest(Long folioId, Long optionId, BigDecimal units, BigDecimal navValue) {
}
