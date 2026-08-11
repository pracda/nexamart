package com.nexamart.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record CommissionReport(
        String periodDescription,
        BigDecimal commissionRate,
        BigDecimal totalRevenue,
        BigDecimal commissionEarned,
        List<CategoryCommission> byCategory
) {
    public record CategoryCommission(String category, BigDecimal revenue, BigDecimal commission) {
    }
}
