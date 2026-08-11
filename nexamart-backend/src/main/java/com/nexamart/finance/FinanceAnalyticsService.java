package com.nexamart.finance;

import com.nexamart.finance.dto.CommissionReport;
import com.nexamart.finance.dto.SellerPayoutsReport;
import com.nexamart.order.OrderItem;
import com.nexamart.order.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Commission and seller-payout figures are derived on the fly from real Order/OrderItem data
 * using a fixed platform commission rate — there is no separate payment/payout ledger in this
 * milestone. This is a documented simplification, not fabricated data: every dollar figure here
 * traces back to a real order.
 */
@Service
public class FinanceAnalyticsService {

    public static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10");

    private final OrderItemRepository orderItemRepository;

    public FinanceAnalyticsService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public CommissionReport commissionReport(Instant from, Instant to, String category) {
        List<OrderItem> items = orderItemRepository.findAllInRange(from, to);
        if (category != null && !category.isBlank()) {
            items = items.stream()
                    .filter(i -> i.getProduct().getCategory().getName().equalsIgnoreCase(category))
                    .toList();
        }

        Map<String, BigDecimal> revenueByCategory = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal lineRevenue = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineRevenue);
            revenueByCategory.merge(item.getProduct().getCategory().getName(), lineRevenue, BigDecimal::add);
        }

        List<CommissionReport.CategoryCommission> byCategory = revenueByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CommissionReport.CategoryCommission(e.getKey(), e.getValue(), commission(e.getValue())))
                .toList();

        return new CommissionReport(describePeriod(from, to), COMMISSION_RATE, total, commission(total), byCategory);
    }

    public SellerPayoutsReport sellerPayoutsReport(Instant from, Instant to) {
        List<OrderItem> items = orderItemRepository.findAllInRange(from, to);

        Map<String, BigDecimal> revenueBySeller = new LinkedHashMap<>();
        for (OrderItem item : items) {
            BigDecimal lineRevenue = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            revenueBySeller.merge(item.getProduct().getSeller().getFullName(), lineRevenue, BigDecimal::add);
        }

        List<SellerPayoutsReport.SellerPayout> payouts = revenueBySeller.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new SellerPayoutsReport.SellerPayout(e.getKey(), e.getValue(), payout(e.getValue())))
                .toList();

        return new SellerPayoutsReport(describePeriod(from, to), COMMISSION_RATE, payouts);
    }

    BigDecimal totalRevenueInRange(Instant from, Instant to) {
        return orderItemRepository.findAllInRange(from, to).stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal commission(BigDecimal revenue) {
        return revenue.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal payout(BigDecimal revenue) {
        return revenue.multiply(BigDecimal.ONE.subtract(COMMISSION_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    private String describePeriod(Instant from, Instant to) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE.withZone(java.time.ZoneOffset.UTC);
        String fromStr = from != null ? fmt.format(from) : "the beginning";
        String toStr = to != null ? fmt.format(to) : "now";
        return "from " + fromStr + " to " + toStr;
    }
}
