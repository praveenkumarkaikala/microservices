package com.fundmatrix.navaccounting.domain;

import com.fundmatrix.navaccounting.common.BaseEntity;
import com.fundmatrix.navaccounting.domain.enums.DividendStatus;
import com.fundmatrix.navaccounting.domain.enums.OptionType;
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
 * A dividend declaration at scheme-option level: dividend per unit as of a record date,
 * from which investor-wise entitlements are computed.
 *
 * <p>{@code schemeId}/{@code optionId} replace the monolith's {@code @ManyToOne}
 * relations. {@code schemeName} and {@code optionType} are snapshotted from
 * FundCatalogClient at declare() time (rather than re-fetched on every read/process),
 * since optionType directly drives the reinvestment-vs-payout branch in
 * {@code DividendService.process()} and must not change mid-lifecycle of a declaration.
 */
@Entity
@Table(name = "dividend_declarations", indexes = {
        @Index(name = "idx_dividend_option", columnList = "option_id"),
        @Index(name = "idx_dividend_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendDeclaration extends BaseEntity {

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "scheme_name", length = 160)
    private String schemeName;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", length = 30)
    private OptionType optionType;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "dividend_per_unit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dividendPerUnit;

    @Column(name = "total_distribution_amount", precision = 19, scale = 2)
    private BigDecimal totalDistributionAmount;

    @Column(name = "declared_by_id")
    private Long declaredById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DividendStatus status;
}
