package com.nexamart.ai.dto;

import java.math.BigDecimal;

public record PricingAdvice(BigDecimal recommendedMin, BigDecimal recommendedMax, String justification) {
}
