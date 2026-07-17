package com.fundmatrix.audit.domain;

import com.fundmatrix.audit.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Immutable audit trail entry capturing who did what to which record and when. Written for
 * all transaction and folio modification actions to support compliance and traceability.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, record_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    /** Id of the acting user (may be null for system actions). */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 120)
    private String userName;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(name = "entity_type", length = 60)
    private String entityType;

    @Column(name = "record_id", length = 60)
    private String recordId;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private Instant timestamp;
}
