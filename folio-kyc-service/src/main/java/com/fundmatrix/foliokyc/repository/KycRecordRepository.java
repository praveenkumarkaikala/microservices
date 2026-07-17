package com.fundmatrix.foliokyc.repository;

import com.fundmatrix.foliokyc.domain.KycRecord;
import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {

    List<KycRecord> findByInvestorId(Long investorId);

    List<KycRecord> findByKycStatus(KycStatus kycStatus);

    /** True when the investor has at least one KYC record in the given status. */
    boolean existsByInvestorIdAndKycStatus(Long investorId, KycStatus kycStatus);

    long countByKycStatus(KycStatus kycStatus);
}
