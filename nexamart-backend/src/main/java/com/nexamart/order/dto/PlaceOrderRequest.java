package com.nexamart.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(@NotEmpty @Valid List<OrderLine> items) {
    public record OrderLine(Long productId, Integer quantity) {
    }
}
