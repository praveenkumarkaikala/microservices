package com.fundmatrix.transaction.repository;

import com.fundmatrix.transaction.domain.TransactionFlag;
import com.fundmatrix.transaction.domain.enums.FlagStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionFlagRepository extends JpaRepository<TransactionFlag, Long> {

    List<TransactionFlag> findAllByOrderByCreatedDateDesc();

    List<TransactionFlag> findByStatusOrderByCreatedDateDesc(FlagStatus status);

    boolean existsByTransaction_Id(Long transactionId);

    long countByStatus(FlagStatus status);
}
