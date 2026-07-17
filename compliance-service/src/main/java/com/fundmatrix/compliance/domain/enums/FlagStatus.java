package com.fundmatrix.compliance.domain.enums;

/**
 * Review lifecycle of a transaction compliance flag. The flag entity itself is owned by
 * transaction-service; this enum only exists here to type the status field of the
 * {@link com.fundmatrix.compliance.dto.TransactionFlagDto} Feign response shape.
 */
public enum FlagStatus {
    OPEN,
    REVIEWED,
    CLEARED,
    ESCALATED
}
