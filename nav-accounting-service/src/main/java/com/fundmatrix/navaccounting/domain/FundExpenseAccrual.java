package com.fundmatrix.navaccounting.domain;

import com.fundmatrix.navaccounting.common.BaseEntity;
import com.fundmatrix.navaccounting.domain.enums.ExpenseStatus;
import com.fundmatrix.navaccounting.domain.enums.ExpenseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A fund-level expense accrual (management fee, trustee fee, audit, custody, distribution)
 * booked against a scheme on a given date at an annualised rate. Owned entirely by this
 * service; {@code schemeId}/{@code schemeName} replace the monolith's {@code @ManyToOne
 * FundScheme} since FundScheme now lives in fund-catalog-service.
 */
@Entity
@Table(name = "fund_expense_accruals", indexes = {
        @Index(name = "idx_accrual_scheme_date", columnList = "scheme_id, accrual_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundExpenseAccrual extends BaseEntity {

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "scheme_name", length = 160)
    private String schemeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 30)
    private ExpenseType expenseType;

    @Column(name = "accrual_amount", precision = 19, scale = 2)
    private BigDecimal accrualAmount;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "annualised_rate", precision = 9, scale = 4)
    private BigDecimal annualisedRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status;

    /** Mandatory reason captured when an accrual is reversed. */
    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;
}
