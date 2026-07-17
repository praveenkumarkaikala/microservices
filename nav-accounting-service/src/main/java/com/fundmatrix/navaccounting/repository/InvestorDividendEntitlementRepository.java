package com.fundmatrix.navaccounting.repository;

import com.fundmatrix.navaccounting.domain.InvestorDividendEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestorDividendEntitlementRepository
        extends JpaRepository<InvestorDividendEntitlement, Long> {

    List<InvestorDividendEntitlement> findByDeclarationId(Long declarationId);

    List<InvestorDividendEntitlement> findByFolioId(Long folioId);

    /**
     * "My entitlements" resolution: since {@code folio} is no longer a JPA relation (folios
     * live in folio-transaction-service), we can't do a derived join to the current user's
     * id. Instead the service layer first resolves the caller's own folio ids via
     * {@code FolioTransactionClient} (the pre-existing GET /folios endpoint, which already
     * returns "folios for the current authenticated user" and works here unchanged because
     * FeignConfig forwards the original Authorization header), then filters entitlements by
     * that folio-id list.
     */
    List<InvestorDividendEntitlement> findByFolioIdInOrderByIdDesc(List<Long> folioIds);

    long countByDeclarationId(Long declarationId);
}
