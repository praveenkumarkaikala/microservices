package com.fundmatrix.distributorcommission.domain;

import com.fundmatrix.distributorcommission.common.BaseEntity;
import com.fundmatrix.distributorcommission.domain.enums.CommissionStatus;
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
import java.time.LocalDate;

/**
 * Trail commission computed for a distributor on a scheme for a billing period, based on
 * the AUM managed and the applicable trail rate. The distributor relation stays a local JPA
 * relation (Distributor is owned by this service); the scheme lives in fund-catalog-service,
 * so it is referenced by a plain {@code schemeId} column and resolved via {@code FundCatalogClient}
 * when scheme name/details are needed.
 */
@Entity
@Table(name = "trail_commissions", indexes = {
        @Index(name = "idx_commission_distributor", columnList = "distributor_id"),
        @Index(name = "idx_commission_period", columnList = "period"),
        @Index(name = "idx_commission_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrailCommission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    /** Billing period in {@code YYYY-MM} format. */
    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "aum_managed", precision = 19, scale = 2)
    private BigDecimal aumManaged;

    @Column(name = "trail_rate", precision = 9, scale = 4)
    private BigDecimal trailRate;

    @Column(name = "commission_amount", precision = 19, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "payout_date")
    private LocalDate payoutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionStatus status;
}
