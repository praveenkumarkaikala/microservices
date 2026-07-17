package com.fundmatrix.distributorcommission.repository;

import com.fundmatrix.distributorcommission.domain.TrailCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrailCommissionRepository extends JpaRepository<TrailCommission, Long> {

    List<TrailCommission> findByDistributor_IdOrderByPeriodDesc(Long distributorId);

    List<TrailCommission> findByPeriod(String period);

    /**
     * Renamed from findByDistributor_IdAndScheme_IdAndPeriod: TrailCommission.scheme (ManyToOne)
     * became a plain schemeId column, since FundScheme now lives in fund-catalog-service.
     */
    Optional<TrailCommission> findByDistributor_IdAndSchemeIdAndPeriod(
            Long distributorId, Long schemeId, String period);
}
