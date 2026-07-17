package com.fundmatrix.transaction.service;

import com.fundmatrix.transaction.client.FolioKycClient;
import com.fundmatrix.transaction.common.exception.ResourceNotFoundException;
import com.fundmatrix.transaction.domain.enums.Role;
import com.fundmatrix.transaction.dto.FolioDto;
import com.fundmatrix.transaction.security.CurrentUserService;
import org.springframework.stereotype.Service;

/**
 * Re-derives the old monolith's FolioService.loadAccessible() role-based visibility rule
 * locally, now against a {@link FolioDto} fetched via {@link FolioKycClient#getFolio} instead
 * of a local InvestorFolio JPA entity (InvestorFolio now lives entirely in folio-kyc-service).
 * Shared by TransactionService/SipService/SwpService exactly as the old in-process FolioService
 * was shared by their monolith-era equivalents.
 *
 * KNOWN LIMITATION (carried over from the old folio-transaction-service's FolioService
 * javadoc): distributor-commission-service (which owns Distributor) is not a contracted Feign
 * consumer of this service, so there is no way to resolve "which Distributor record belongs to
 * the current DISTRIBUTOR-role user". This assumes the JWT-carried user id can be used directly
 * as distributorId for DISTRIBUTOR-role scoping.
 */
@Service
public class FolioAccessService {

    private final FolioKycClient folioKycClient;
    private final CurrentUserService currentUser;

    public FolioAccessService(FolioKycClient folioKycClient, CurrentUserService currentUser) {
        this.folioKycClient = folioKycClient;
        this.currentUser = currentUser;
    }

    /** Loads a folio enforcing role-based visibility; hides existence on access denial. */
    public FolioDto loadAccessible(Long id) {
        FolioDto folio = folioKycClient.getFolio(id);
        if (folio == null) {
            throw ResourceNotFoundException.of("InvestorFolio", id);
        }
        Role role = currentUser.getRole();
        if (role == Role.INVESTOR && !folio.investorId().equals(currentUser.getId())) {
            throw ResourceNotFoundException.of("InvestorFolio", id);
        }
        if (role == Role.DISTRIBUTOR) {
            // See class-level note: distributorId is assumed == the DISTRIBUTOR user's id.
            boolean owns = folio.distributorId() != null
                    && folio.distributorId().equals(currentUser.getId());
            if (!owns) {
                throw ResourceNotFoundException.of("InvestorFolio", id);
            }
        }
        return folio;
    }
}
