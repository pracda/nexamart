package com.nexamart.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlatformRevenueReport(
        String periodDescription,
        BigDecimal totalRevenue,
        int orderCount,
        List<CategoryRevenue> byCategory
) {
    public record CategoryRevenue(String category, BigDecimal revenue) {
    }
}
