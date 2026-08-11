package com.nexamart.finance.dto;

import java.math.BigDecimal;

public record AnomalyAlert(
        String currentPeriodDescription,
        BigDecimal currentPeriodRevenue,
        String previousPeriodDescription,
        BigDecimal previousPeriodRevenue,
        boolean significant,
        String message
) {
}
