package com.fundmatrix.foliokyc.repository;

import com.fundmatrix.foliokyc.domain.InvestorFolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestorFolioRepository extends JpaRepository<InvestorFolio, Long> {

    Optional<InvestorFolio> findByFolioNumber(String folioNumber);

    boolean existsByFolioNumber(String folioNumber);

    List<InvestorFolio> findByInvestorId(Long investorId);

    List<InvestorFolio> findByDistributorId(Long distributorId);

    long countByDistributorId(Long distributorId);
}
