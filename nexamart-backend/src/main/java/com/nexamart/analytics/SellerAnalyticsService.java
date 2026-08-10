package com.nexamart.analytics;

import com.nexamart.analytics.dto.SellerSalesSummary;
import com.nexamart.order.OrderItem;
import com.nexamart.order.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SellerAnalyticsService {

    private final OrderItemRepository orderItemRepository;

    public SellerAnalyticsService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public SellerSalesSummary salesSummary(Long sellerId, Instant from, Instant to, int topN) {
        List<OrderItem> items = orderItemRepository.findForSeller(sellerId, from, to);

        Map<Long, SellerSalesSummary.ProductSales> byProduct = new LinkedHashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalUnits = 0;

        for (OrderItem item : items) {
            BigDecimal lineRevenue = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalRevenue = totalRevenue.add(lineRevenue);
            totalUnits += item.getQuantity();

            Long productId = item.getProduct().getId();
            SellerSalesSummary.ProductSales existing = byProduct.get(productId);
            if (existing == null) {
                byProduct.put(productId, new SellerSalesSummary.ProductSales(
                        productId, item.getProductTitle(), item.getQuantity(), lineRevenue));
            } else {
                byProduct.put(productId, new SellerSalesSummary.ProductSales(
                        productId, existing.title(), existing.unitsSold() + item.getQuantity(),
                        existing.revenue().add(lineRevenue)));
            }
        }

        List<SellerSalesSummary.ProductSales> topProducts = byProduct.values().stream()
                .sorted(Comparator.comparing(SellerSalesSummary.ProductSales::revenue).reversed())
                .limit(topN)
                .toList();

        long orderCount = items.stream().map(i -> i.getOrder().getId()).distinct().count();

        return new SellerSalesSummary(describePeriod(from, to), totalRevenue, totalUnits, (int) orderCount, topProducts);
    }

    private String describePeriod(Instant from, Instant to) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE.withZone(java.time.ZoneOffset.UTC);
        String fromStr = from != null ? fmt.format(from) : "the beginning";
        String toStr = to != null ? fmt.format(to) : "now";
        return "from " + fromStr + " to " + toStr;
    }
}
