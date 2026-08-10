package com.nexamart.order;

import com.nexamart.auth.User;
import com.nexamart.catalog.Product;
import com.nexamart.catalog.ProductRepository;
import com.nexamart.common.ApiException;
import com.nexamart.order.dto.OrderResponse;
import com.nexamart.order.dto.PlaceOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req, User buyer) {
        BigDecimal total = BigDecimal.ZERO;
        String estimatedDelivery = DateTimeFormatter.ISO_LOCAL_DATE
                .format(Instant.now().plus(5, ChronoUnit.DAYS).atZone(java.time.ZoneOffset.UTC));

        Order order = new Order(buyer, BigDecimal.ZERO, estimatedDelivery);

        for (PlaceOrderRequest.OrderLine line : req.items()) {
            if (line.quantity() == null || line.quantity() <= 0) {
                throw ApiException.badRequest("Quantity must be positive for product " + line.productId());
            }
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound("Product not found: " + line.productId()));
            if (product.getStockQuantity() < line.quantity()) {
                throw ApiException.badRequest("Insufficient stock for " + product.getTitle());
            }
            product.setStockQuantity(product.getStockQuantity() - line.quantity());
            productRepository.save(product);

            OrderItem item = new OrderItem(product, line.quantity(), product.getPrice());
            order.addItem(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        order.setTotalAmount(total);
        return OrderResponse.from(orderRepository.save(order));
    }

    public List<OrderResponse> myOrders(Long buyerId) {
        return orderRepository.findByBuyer_IdOrderByCreatedAtDesc(buyerId).stream()
                .map(OrderResponse::from).toList();
    }

    public OrderResponse getForBuyer(Long orderId, Long buyerId) {
        return OrderResponse.from(orderRepository.findByIdAndBuyer_Id(orderId, buyerId)
                .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId)));
    }

    public OrderResponse getById(Long orderId) {
        return OrderResponse.from(findOrThrow(orderId));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = findOrThrow(orderId);
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order findOrThrow(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
    }
}
