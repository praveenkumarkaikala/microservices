package com.fundmatrix.navaccounting.client;

/**
 * Minimal projection of folio-transaction-service's FolioDto - only {@code id} is used here
 * (to resolve the current investor's folio ids for DividendService.myEntitlements()). Not
 * part of the FEIGN_CONTRACTS.md "ADD" list: this hits the monolith's pre-existing
 * GET /folios endpoint (FolioController.list(), unchanged - "existing FolioController stays"),
 * which already returns "folios for the current authenticated user" - it works here because
 * FeignConfig forwards the caller's original Authorization header.
 */
public record FolioDto(Long id) {
}
