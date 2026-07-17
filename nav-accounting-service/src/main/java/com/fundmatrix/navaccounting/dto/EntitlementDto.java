package com.fundmatrix.navaccounting.dto;

import com.fundmatrix.navaccounting.domain.enums.EntitlementStatus;
import com.fundmatrix.navaccounting.domain.enums.PayoutMode;

import java.math.BigDecimal;

/**
 * folioNumber/investorName were dropped relative to the monolith's DTO: they came from the
 * InvestorFolio/User JPA graph which this service no longer has direct access to (folio and
 * user data now live in folio-transaction-service / auth-user-service respectively, and there
 * is no bulk "resolve folio numbers" Feign endpoint in the contract). folioId/investorId are
 * kept so a consumer can resolve display fields itself if needed.
 */
public record EntitlementDto(
        Long id,
        Long declarationId,
        Long folioId,
        Long investorId,
        BigDecimal unitsOnRecordDate,
        BigDecimal grossDividend,
        BigDecimal taxDeducted,
        BigDecimal netDividend,
        PayoutMode payoutMode,
        EntitlementStatus status
) {
}
