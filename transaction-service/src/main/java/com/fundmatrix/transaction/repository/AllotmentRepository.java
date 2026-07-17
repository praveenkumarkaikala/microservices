package com.fundmatrix.transaction.repository;

import com.fundmatrix.transaction.domain.Allotment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllotmentRepository extends JpaRepository<Allotment, Long> {

    Optional<Allotment> findByTransaction_Id(Long transactionId);
}
