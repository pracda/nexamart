package com.nexamart.dispute.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMessageRequest(@NotBlank String body) {
}
