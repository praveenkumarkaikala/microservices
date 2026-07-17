package com.fundmatrix.transaction.repository;

import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionRef(String transactionRef);

    List<Transaction> findByFolioIdOrderByTransactionDateDesc(Long folioId);

    List<Transaction> findByInvestorIdOrderByTransactionDateDesc(Long investorId);

    List<Transaction> findByDistributorIdOrderByTransactionDateDesc(Long distributorId);

    List<Transaction> findByStatusOrderByTransactionDateAsc(TransactionStatus status);

    List<Transaction> findByStatusInOrderByTransactionDateAsc(List<TransactionStatus> statuses);
}
