package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.domain.enums.KycType;

import java.time.LocalDate;

/**
 * Note: unlike the monolith's KycRecordDto, this no longer carries {@code investorName} -
 * User is owned by auth-user-service and this service does not join across services just to
 * enrich a display label.
 */
public record KycRecordDto(
        Long id,
        Long investorId,
        KycType kycType,
        String documentType,
        String documentRef,
        LocalDate verifiedDate,
        KycStatus kycStatus
) {
}
