package com.nexamart.dispute.dto;

import com.nexamart.dispute.DisputeStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveDisputeRequest(@NotNull DisputeStatus status, String resolutionNote) {
}
