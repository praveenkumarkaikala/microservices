package com.fundmatrix.authuser.dto;

import com.fundmatrix.authuser.domain.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
