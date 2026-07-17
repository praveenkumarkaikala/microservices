package com.fundmatrix.transaction.repository;

import com.fundmatrix.transaction.domain.SwpMandate;
import com.fundmatrix.transaction.domain.enums.SipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SwpMandateRepository extends JpaRepository<SwpMandate, Long> {

    List<SwpMandate> findByFolioId(Long folioId);

    List<SwpMandate> findByInvestorId(Long investorId);

    List<SwpMandate> findByStatus(SipStatus status);
}
