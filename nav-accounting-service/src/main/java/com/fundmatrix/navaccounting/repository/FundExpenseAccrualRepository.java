package com.fundmatrix.navaccounting.repository;

import com.fundmatrix.navaccounting.domain.FundExpenseAccrual;
import com.fundmatrix.navaccounting.domain.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundExpenseAccrualRepository extends JpaRepository<FundExpenseAccrual, Long> {

    List<FundExpenseAccrual> findBySchemeIdOrderByAccrualDateDesc(Long schemeId);

    List<FundExpenseAccrual> findBySchemeIdAndStatus(Long schemeId, ExpenseStatus status);
}
