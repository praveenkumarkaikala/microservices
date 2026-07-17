package com.fundmatrix.distributorcommission.client;

/** Minimal projection of fund-catalog-service's FundScheme, as returned by GET /schemes/{id}. */
public record SchemeDto(
        Long id,
        String schemeName,
        String schemeCode
) {
}
