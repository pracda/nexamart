package com.nexamart.catalog.dto;

import com.nexamart.catalog.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        String category,
        Long sellerId,
        String sellerName
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getTitle(), p.getDescription(), p.getPrice(), p.getStockQuantity(),
                p.getImageUrl(), p.getCategory().getName(), p.getSeller().getId(), p.getSeller().getFullName()
        );
    }
}
