package com.fundmatrix.fundcatalog.domain;

import com.fundmatrix.fundcatalog.common.BaseEntity;
import com.fundmatrix.fundcatalog.domain.enums.RiskProfile;
import com.fundmatrix.fundcatalog.domain.enums.SchemeCategory;
import com.fundmatrix.fundcatalog.domain.enums.SchemeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@Table(name = "fund_schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundScheme extends BaseEntity {

    @Column(name = "scheme_name", nullable = false, length = 160)
    private String schemeName;

    @Column(name = "scheme_code", nullable = false, unique = true, length = 40)
    private String schemeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchemeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", nullable = false, length = 20)
    private RiskProfile riskProfile;

    @Column(name = "benchmark_index", length = 120)
    private String benchmarkIndex;

    /** Reference to the managing fund manager (AMC staff). */
    @Column(name = "fund_manager_id")
    private Long fundManagerId;

    @Column(name = "fund_manager_name", length = 120)
    private String fundManagerName;

    @Column(name = "min_investment", precision = 19, scale = 2)
    private BigDecimal minInvestment;

    /** Human-readable exit-load slab description, e.g. "1% if redeemed within 365 days". */
    @Column(name = "exit_load_slab", length = 255)
    private String exitLoadSlab;

    /** Exit-load percentage applied within the load period. */
    @Column(name = "exit_load_rate", precision = 9, scale = 4)
    private BigDecimal exitLoadRate;

    /** Number of days for which the exit load applies. */
    @Column(name = "exit_load_period_days")
    private Integer exitLoadPeriodDays;

    @Column(name = "expense_ratio", precision = 9, scale = 4)
    private BigDecimal expenseRatio;

    /** Minimum amount per SIP instalment for this scheme (rule config). */
    @Column(name = "min_sip_amount", precision = 19, scale = 2)
    private BigDecimal minSipAmount;

    /** Minimum amount per SWP instalment for this scheme (rule config). */
    @Column(name = "min_swp_amount", precision = 19, scale = 2)
    private BigDecimal minSwpAmount;

    /** Per-scheme subscription cut-off ("HH:mm"); overrides the global cut-off when set. */
    @Column(name = "cutoff_time", length = 5)
    private String cutoffTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchemeStatus status;
}
