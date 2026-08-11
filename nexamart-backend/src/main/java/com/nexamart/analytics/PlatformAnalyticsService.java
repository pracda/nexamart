package com.nexamart.analytics;

import com.nexamart.analytics.dto.DisputeStatsReport;
import com.nexamart.analytics.dto.PlatformRevenueReport;
import com.nexamart.dispute.Dispute;
import com.nexamart.dispute.DisputeRepository;
import com.nexamart.order.OrderItem;
import com.nexamart.order.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlatformAnalyticsService {

    private final OrderItemRepository orderItemRepository;
    private final DisputeRepository disputeRepository;

    public PlatformAnalyticsService(OrderItemRepository orderItemRepository, DisputeRepository disputeRepository) {
        this.orderItemRepository = orderItemRepository;
        this.disputeRepository = disputeRepository;
    }

    public PlatformRevenueReport revenueReport(Instant from, Instant to) {
        List<OrderItem> items = orderItemRepository.findAllInRange(from, to);

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal lineRevenue = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineRevenue);
            byCategory.merge(item.getProduct().getCategory().getName(), lineRevenue, BigDecimal::add);
        }

        List<PlatformRevenueReport.CategoryRevenue> categoryList = byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new PlatformRevenueReport.CategoryRevenue(e.getKey(), e.getValue()))
                .toList();

        long orderCount = items.stream().map(i -> i.getOrder().getId()).distinct().count();

        return new PlatformRevenueReport(describePeriod(from, to), total, (int) orderCount, categoryList);
    }

    public DisputeStatsReport disputeStats(Instant from, Instant to) {
        List<Dispute> inRange = disputeRepository.findAll().stream()
                .filter(d -> (from == null || !d.getCreatedAt().isBefore(from))
                        && (to == null || !d.getCreatedAt().isAfter(to)))
                .toList();

        Map<String, Long> byStatus = inRange.stream()
                .collect(Collectors.groupingBy(d -> d.getStatus().name(), LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> bySeller = new LinkedHashMap<>();
        for (Dispute d : inRange) {
            Set<String> sellersOnThisDispute = d.getOrder().getItems().stream()
                    .map(item -> item.getProduct().getSeller().getFullName())
                    .collect(Collectors.toSet());
            for (String seller : sellersOnThisDispute) {
                bySeller.merge(seller, 1L, Long::sum);
            }
        }
        List<DisputeStatsReport.SellerDisputeCount> topSellers = bySeller.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new DisputeStatsReport.SellerDisputeCount(e.getKey(), e.getValue()))
                .toList();

        return new DisputeStatsReport(describePeriod(from, to), inRange.size(), byStatus, topSellers);
    }

    private String describePeriod(Instant from, Instant to) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE.withZone(java.time.ZoneOffset.UTC);
        String fromStr = from != null ? fmt.format(from) : "the beginning";
        String toStr = to != null ? fmt.format(to) : "now";
        return "from " + fromStr + " to " + toStr;
    }
}
