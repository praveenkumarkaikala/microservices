package com.fundmatrix.foliokyc.dto;

import java.util.List;

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
         List<NomineeDetails> nomineeDetails,
        @Size(max = 60) String bankAccountRef
) {
}
