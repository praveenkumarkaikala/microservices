package com.fundmatrix.navaccounting.domain;

import com.fundmatrix.navaccounting.common.BaseEntity;
import com.fundmatrix.navaccounting.domain.enums.EntitlementStatus;
import com.fundmatrix.navaccounting.domain.enums.PayoutMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * An individual investor's dividend entitlement derived from a {@link DividendDeclaration}:
 * units on record date, gross dividend, tax deducted, net dividend and payout mode.
 *
 * <p>{@code declaration} stays a real {@code @ManyToOne} (intra-service - both entities are
 * owned here). {@code folio} becomes a plain {@code folioId} since InvestorFolio now lives in
 * folio-transaction-service. {@code investorId} is a deliberate addition (not present on the
 * monolith's entity): it is snapshotted from {@code HoldingDto.investorId()} while computing
 * entitlements in {@code computeEntitlements()} so that {@code process()} can notify the
 * correct investor for BOTH the reinvestment and bank-credit branches without an extra Feign
 * call per entitlement (HoldingDto only carries investorId at the point where holdings are
 * fetched by option; there is no "folio -> investor" lookup in the Feign contract).
 */
@Entity
@Table(name = "investor_dividend_entitlements", indexes = {
        @Index(name = "idx_entitlement_declaration", columnList = "declaration_id"),
        @Index(name = "idx_entitlement_folio", columnList = "folio_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorDividendEntitlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "declaration_id", nullable = false)
    private DividendDeclaration declaration;

    @Column(name = "folio_id", nullable = false)
    private Long folioId;

    @Column(name = "investor_id")
    private Long investorId;

    @Column(name = "units_on_record_date", precision = 19, scale = 4)
    private BigDecimal unitsOnRecordDate;

    @Column(name = "gross_dividend", precision = 19, scale = 2)
    private BigDecimal grossDividend;

    @Column(name = "tax_deducted", precision = 19, scale = 2)
    private BigDecimal taxDeducted;

    @Column(name = "net_dividend", precision = 19, scale = 2)
    private BigDecimal netDividend;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_mode", nullable = false, length = 20)
    private PayoutMode payoutMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntitlementStatus status;
}
