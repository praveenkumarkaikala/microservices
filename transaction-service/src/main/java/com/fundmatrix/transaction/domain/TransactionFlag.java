package com.fundmatrix.transaction.domain;

import com.fundmatrix.transaction.common.BaseEntity;
import com.fundmatrix.transaction.domain.enums.FlagStatus;
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
 * A compliance flag raised against a transaction (e.g. high-value). Moves through a review
 * workflow: OPEN → REVIEWED → CLEARED / ESCALATED. Reviewed remotely by compliance-service
 * via GET/PATCH /transactions/flags** (see FEIGN_CONTRACTS.md).
 */
@Entity
@Table(name = "transaction_flags", indexes = {
        @Index(name = "idx_flag_status", columnList = "status"),
        @Index(name = "idx_flag_txn", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFlag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false, length = 255)
    private String reason;

    /** Snapshot of the transaction amount at flag time. */
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlagStatus status;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "reviewed_date")
    private Instant reviewedDate;
}
