package com.nexamart.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record SellerSalesSummary(
        String periodDescription,
        BigDecimal totalRevenue,
        int totalUnitsSold,
        int orderCount,
        List<ProductSales> topProducts
) {
    public record ProductSales(Long productId, String title, int unitsSold, BigDecimal revenue) {
    }
}
