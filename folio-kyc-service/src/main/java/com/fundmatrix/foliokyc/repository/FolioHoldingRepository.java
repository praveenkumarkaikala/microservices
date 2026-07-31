package com.fundmatrix.foliokyc.repository;

import com.fundmatrix.foliokyc.domain.FolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FolioHoldingRepository extends JpaRepository<FolioHolding, Long> {

    List<FolioHolding> findByFolio_Id(Long folioId);

    List<FolioHolding> findBySchemeId(Long schemeId);

    List<FolioHolding> findByOptionId(Long optionId);
    
    List<FolioHolding> findByFolio_IdIn(List<Long> ids);
    
    List<FolioHolding> findByFolio_InvestorId(Long investorId);

    Optional<FolioHolding> findByFolio_IdAndOptionId(Long folioId, Long optionId);

    /** Holdings in a given scheme option held under folios serviced by a distributor. */
    List<FolioHolding> findByFolio_DistributorIdAndSchemeId(Long distributorId, Long schemeId);

    @Query("select coalesce(sum(h.currentValue), 0) from FolioHolding h " +
            "where h.folio.distributorId = :distributorId and h.schemeId = :schemeId")
    BigDecimal sumCurrentValueByDistributorAndScheme(@Param("distributorId") Long distributorId,
                                                     @Param("schemeId") Long schemeId);

    @Query("select coalesce(sum(h.currentValue), 0) from FolioHolding h where h.schemeId = :schemeId")
    BigDecimal sumCurrentValueByScheme(@Param("schemeId") Long schemeId);

    @Query("select coalesce(sum(h.currentValue), 0) from FolioHolding h " +
            "where h.folio.distributorId = :distributorId")
    BigDecimal sumCurrentValueByDistributor(@Param("distributorId") Long distributorId);
}
