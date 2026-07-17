package com.fundmatrix.dashboard.client;

/** Own lean copy of folio-transaction-service's FolioDto - only the fields dashboard-service needs. */
public record FolioDto(Long id, String folioNumber, Long investorId, Long distributorId, String status) {
}
