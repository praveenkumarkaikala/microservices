package com.fundmatrix.foliokyc.domain;

import com.fundmatrix.foliokyc.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "folio_holdings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolioHolding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    private InvestorFolio folio;

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "units_held", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitsHeld;

    @Column(name = "average_cost_nav", precision = 19, scale = 4)
    private BigDecimal averageCostNav;

    @Column(name = "current_value", precision = 19, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "unrealised_gain_loss", precision = 19, scale = 2)
    private BigDecimal unrealisedGainLoss;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
