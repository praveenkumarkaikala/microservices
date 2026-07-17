package com.fundmatrix.transaction.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Batch allotment request — a list of transaction ids to allot in one go. */
public record BatchAllotRequest(
        @NotEmpty List<Long> transactionIds
) {
}
