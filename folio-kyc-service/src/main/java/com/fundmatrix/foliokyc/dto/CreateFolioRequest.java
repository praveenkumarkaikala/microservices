package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.ModeOfHolding;
import com.fundmatrix.foliokyc.domain.enums.TaxStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFolioRequest(
        /** Optional — when omitted, the folio is created for the authenticated investor. */
        Long investorId,
        Long distributorId,
        @NotNull TaxStatus taxStatus,
        @NotNull ModeOfHolding modeOfHolding,
        @Size(max = 255) String nomineeDetails,
        @Size(max = 60) String bankAccountRef
) {
}
