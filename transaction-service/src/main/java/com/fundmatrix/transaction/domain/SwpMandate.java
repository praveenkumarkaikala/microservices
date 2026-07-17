package com.fundmatrix.transaction.domain;

import com.fundmatrix.transaction.common.BaseEntity;
import com.fundmatrix.transaction.domain.enums.SipFrequency;
import com.fundmatrix.transaction.domain.enums.SipStatus;
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
 * A Systematic Withdrawal Plan mandate: a recurring redemption instruction for a
 * folio/scheme/option at a fixed amount and frequency. Reuses {@link SipFrequency} and
 * {@link SipStatus} (same value sets). Folio referenced by plain id + denormalized snapshot,
 * same rationale as {@link SipMandate}.
 */
@Entity
@Table(name = "swp_mandates", indexes = {
        @Index(name = "idx_swp_folio", columnList = "folio_id"),
        @Index(name = "idx_swp_investor", columnList = "investor_id"),
        @Index(name = "idx_swp_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwpMandate extends BaseEntity {

    @Column(name = "mandate_ref", unique = true, length = 30)
    private String mandateRef;

    @Column(name = "folio_id", nullable = false)
    private Long folioId;

    @Column(name = "folio_number", length = 30)
    private String folioNumber;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** Denormalized snapshot of the folio's servicing distributor id, if any. */
    @Column(name = "distributor_id")
    private Long distributorId;

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    /** Fixed withdrawal amount per instalment. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SipFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "instalment_count")
    private Integer instalmentCount;

    @Column(name = "instalments_executed", nullable = false)
    private Integer instalmentsExecuted;

    @Column(name = "next_instalment_date")
    private LocalDate nextInstalmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SipStatus status;
}
