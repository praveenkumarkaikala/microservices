package com.fundmatrix.transaction.dto;

/**
 * Minimal projection of folio-kyc-service's FolioDto (GET /folios/{id}) - only the fields
 * transaction-service actually needs: folio identity/number for messages and transaction
 * snapshots, and investorId/distributorId/status for the access-control and active-folio
 * checks this service now has to re-derive locally (see FolioKycClient + TransactionService's
 * loadAccessibleFolio()). status is a plain String (not the folio-kyc-service FolioStatus
 * enum type) - DTOs are duplicated per-service per FEIGN_CONTRACTS.md, so this avoids coupling
 * to another service's enum class; compared against the literal "ACTIVE".
 */
public record FolioDto(
        Long id,
        String folioNumber,
        Long investorId,
        Long distributorId,
        String status
) {
}
