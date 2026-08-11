package com.nexamart.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PricingAdviceRequest(@NotBlank String title, @NotBlank String category, BigDecimal draftPrice) {
}
