package com.nexamart.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record SellerPayoutsReport(
        String periodDescription,
        BigDecimal commissionRate,
        List<SellerPayout> payouts
) {
    public record SellerPayout(String sellerName, BigDecimal grossRevenue, BigDecimal payout) {
    }
}
