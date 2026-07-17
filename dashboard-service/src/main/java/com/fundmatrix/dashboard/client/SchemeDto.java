package com.fundmatrix.dashboard.client;

/** Own lean copy of fund-catalog-service's FundSchemeDto - only the fields dashboard-service needs. */
public record SchemeDto(Long id, String schemeName, String schemeCode, String status) {
}
