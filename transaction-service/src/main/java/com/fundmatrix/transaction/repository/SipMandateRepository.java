package com.fundmatrix.transaction.repository;

import com.fundmatrix.transaction.domain.SipMandate;
import com.fundmatrix.transaction.domain.enums.SipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SipMandateRepository extends JpaRepository<SipMandate, Long> {

    Optional<SipMandate> findByMandateRef(String mandateRef);

    List<SipMandate> findByFolioId(Long folioId);

    List<SipMandate> findByInvestorId(Long investorId);

    List<SipMandate> findByStatus(SipStatus status);
}
