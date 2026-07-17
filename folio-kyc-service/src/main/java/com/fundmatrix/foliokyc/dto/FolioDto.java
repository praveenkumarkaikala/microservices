package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.FolioStatus;
import com.fundmatrix.foliokyc.domain.enums.ModeOfHolding;
import com.fundmatrix.foliokyc.domain.enums.TaxStatus;

import java.math.BigDecimal;

/**
 * distributorName is intentionally omitted (simplification): distributor-commission-service
 * is not one of this service's contracted Feign consumers, so only the plain distributorId
 * is surfaced; callers that need the distributor's name can resolve it themselves.
 */
public record FolioDto(
        Long id,
        String folioNumber,
        Long investorId,
        String investorName,
        Long distributorId,
        TaxStatus taxStatus,
        ModeOfHolding modeOfHolding,
        String nomineeDetails,
        String bankAccountRef,
        FolioStatus status,
        BigDecimal currentValue
) {
}
