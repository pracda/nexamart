package com.nexamart.analytics.dto;

import java.util.List;
import java.util.Map;

public record DisputeStatsReport(
        String periodDescription,
        int totalDisputes,
        Map<String, Long> byStatus,
        List<SellerDisputeCount> topSellersByDisputeCount
) {
    public record SellerDisputeCount(String sellerName, long disputeCount) {
    }
}
