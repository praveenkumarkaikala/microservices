package com.fundmatrix.audit.repository;

import com.fundmatrix.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeIgnoreCase(String entityType, Pageable pageable);

    /** Supports the audit-trail read endpoint filtered to a specific entity/record. */
    List<AuditLog> findByEntityTypeAndRecordId(String entityType, String recordId);

    /** Supports the simple "list everything, newest first" audit-trail read endpoint. */
    List<AuditLog> findAllByOrderByTimestampDesc();
}
