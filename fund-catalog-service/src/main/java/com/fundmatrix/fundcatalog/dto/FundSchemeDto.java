package com.fundmatrix.fundcatalog.dto;

import com.fundmatrix.fundcatalog.domain.enums.RiskProfile;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * NOTE: the monolith's version carried a {@code latestNav} convenience field on each nested
 * {@link SchemeOptionDto} (sourced from NavRecordRepository). NavRecord now belongs to
 * nav-accounting-service. Pulling live NAV into every scheme/option read here would mean an
 * N-way Feign fan-out (one call per option) on a plain catalogue listing - a heavy, chatty
 * dependency for a "nice to have" display field. We drop the field from this module's DTOs;
 * services that need NAV alongside scheme data (e.g. folio-transaction) call
 * nav-accounting-service's own GET /nav/latest/{optionId} directly when they need it.
 */
public record FundSchemeDto(
        Long id,
        String schemeName,
        String schemeCode,
        SchemeCategory category,
        RiskProfile riskProfile,
        String benchmarkIndex,
        Long fundManagerId,
        String fundManagerName,
        BigDecimal minInvestment,
        String exitLoadSlab,
        BigDecimal exitLoadRate,
        Integer exitLoadPeriodDays,
        BigDecimal expenseRatio,
        BigDecimal minSipAmount,
        BigDecimal minSwpAmount,
        String cutoffTime,
        SchemeStatus status,
        List<SchemeOptionDto> options
) {
}
