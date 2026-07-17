package com.fundmatrix.distributorcommission.dto;

import com.fundmatrix.distributorcommission.domain.enums.CommissionModel;
import com.fundmatrix.distributorcommission.domain.enums.DistributorStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * userName/userEmail are populated at read time via AuthUserClient (auth-user-service owns the
 * User table) instead of an eager JPA join, since Distributor.user was replaced by a plain userId.
 */
public record DistributorDto(
        Long id,
        String name,
        String arnNumber,
        String euinNumber,
        LocalDate empanelmentDate,
        CommissionModel commissionModel,
        DistributorStatus status,
        Long userId,
        String userName,
        String userEmail,
        BigDecimal aumManaged
) {
}
