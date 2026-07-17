package com.fundmatrix.dashboard.client;

/** Own lean copy of folio-transaction-service's SipMandateDto - only the fields dashboard-service needs. */
public record SipMandateDto(Long id, Long folioId, String status) {
}
