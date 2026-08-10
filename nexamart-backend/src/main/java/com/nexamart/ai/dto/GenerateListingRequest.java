package com.nexamart.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateListingRequest(@NotBlank String productName, String notes) {
}
