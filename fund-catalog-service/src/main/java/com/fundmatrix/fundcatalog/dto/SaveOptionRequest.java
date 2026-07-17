package com.fundmatrix.fundcatalog.dto;

import com.fundmatrix.fundcatalog.domain.enums.OptionStatus;
import com.fundmatrix.fundcatalog.domain.enums.OptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveOptionRequest(
        @NotNull OptionType optionType,
        @Size(max = 20) String isin,
        OptionStatus status
) {
}
