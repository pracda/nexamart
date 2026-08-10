package com.nexamart.order.dto;

import com.nexamart.order.Order;
import com.nexamart.order.OrderItem;
import com.nexamart.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String estimatedDelivery,
        Instant createdAt,
        Instant updatedAt,
        List<Item> items
) {
    public record Item(Long productId, String productTitle, Integer quantity, BigDecimal unitPrice) {
        static Item from(OrderItem i) {
            return new Item(i.getProduct().getId(), i.getProductTitle(), i.getQuantity(), i.getUnitPrice());
        }
    }

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(), o.getStatus(), o.getTotalAmount(), o.getEstimatedDelivery(),
                o.getCreatedAt(), o.getUpdatedAt(),
                o.getItems().stream().map(Item::from).toList()
        );
    }
}
