package com.nexamart.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpenDisputeRequest(@NotNull Long orderId, @NotBlank String reason, @NotBlank String message) {
}
