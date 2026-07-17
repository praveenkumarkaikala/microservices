package com.fundmatrix.transaction.domain;

import com.fundmatrix.transaction.common.BaseEntity;
import com.fundmatrix.transaction.domain.enums.CutOffStatus;
import com.fundmatrix.transaction.domain.enums.TransactionStatus;
import com.fundmatrix.transaction.domain.enums.TransactionType;
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
import java.time.Instant;

/**
 * A financial transaction against a folio — subscription, redemption, switch, SIP/SWP
 * instalment or dividend. NAV applicability is governed by cut-off timing.
 *
 * Cross-service rewrite: InvestorFolio (and FolioHolding) now live entirely in
 * folio-kyc-service, so this entity no longer holds a JPA {@code @ManyToOne} relation to a
 * local folio. Instead it stores plain {@code folioId} plus a denormalized snapshot of
 * {@code folioNumber}/{@code investorId}/{@code distributorId} taken at creation time (via
 * FolioKycClient#getFolio) — this keeps role-scoped listing queries
 * (findByInvestorId/findByDistributorId) working as simple local columns instead of requiring
 * an expensive per-row Feign call. sipMandate stays a real JPA relation since SipMandate
 * remains a local aggregate in this same service.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_ref", columnList = "transaction_ref", unique = true),
        @Index(name = "idx_txn_folio", columnList = "folio_id"),
        @Index(name = "idx_txn_investor", columnList = "investor_id"),
        @Index(name = "idx_txn_distributor", columnList = "distributor_id"),
        @Index(name = "idx_txn_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    /** Human-readable transaction reference, e.g. "TXN00000123"; assigned from the id after insert. */
    @Column(name = "transaction_ref", unique = true, length = 30)
    private String transactionRef;

    @Column(name = "folio_id", nullable = false)
    private Long folioId;

    /** Denormalized snapshot of the folio's human-readable number, for display/messages. */
    @Column(name = "folio_number", length = 30)
    private String folioNumber;

    /** Denormalized snapshot of the folio's owning investor id, for role-scoped listing. */
    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    /** Denormalized snapshot of the folio's servicing distributor id, if any. */
    @Column(name = "distributor_id")
    private Long distributorId;

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    /** Gross monetary amount (for subscriptions; computed proceeds for redemptions). */
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal units;

    @Column(name = "applicable_nav", precision = 19, scale = 4)
    private BigDecimal applicableNav;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cut_off_status", length = 20)
    private CutOffStatus cutOffStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Exit load deducted on redemption, if any. */
    @Column(name = "exit_load_amount", precision = 19, scale = 2)
    private BigDecimal exitLoadAmount;

    /** Free-text remark (rejection reason, switch linkage, SIP reference, etc.). */
    @Column(length = 255)
    private String remarks;

    /** Links a SIP/SWP instalment back to its originating mandate (local aggregate). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sip_mandate_id")
    private SipMandate sipMandate;
}
