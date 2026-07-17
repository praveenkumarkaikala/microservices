package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.FolioStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFolioStatusRequest(@NotNull FolioStatus status) {
}
