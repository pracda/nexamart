package com.nexamart.catalog;

import com.nexamart.auth.User;
import com.nexamart.catalog.dto.ProductRequest;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.common.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductResponse> search(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.search(blankToNull(keyword), blankToNull(category), minPrice, maxPrice)
                .stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getById(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    public List<ProductResponse> listBySeller(Long sellerId) {
        return productRepository.findBySeller_Id(sellerId).stream().map(ProductResponse::from).toList();
    }

    public ProductResponse create(ProductRequest req, User seller) {
        Category category = categoryRepository.findByNameIgnoreCase(req.category())
                .orElseGet(() -> categoryRepository.save(new Category(req.category())));
        Product product = new Product(req.title(), req.description(), req.price(), req.stockQuantity(),
                req.imageUrl(), category, seller);
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest req, User seller) {
        Product product = findOrThrow(id);
        assertOwner(product, seller);
        Category category = categoryRepository.findByNameIgnoreCase(req.category())
                .orElseGet(() -> categoryRepository.save(new Category(req.category())));
        product.setTitle(req.title());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        product.setImageUrl(req.imageUrl());
        product.setCategory(category);
        return ProductResponse.from(productRepository.save(product));
    }

    public void delete(Long id, User seller) {
        Product product = findOrThrow(id);
        assertOwner(product, seller);
        productRepository.delete(product);
    }

    public List<ProductResponse> lowStock(Long sellerId, int threshold) {
        return productRepository.findByStockQuantityLessThan(threshold).stream()
                .filter(p -> p.getSeller().getId().equals(sellerId))
                .map(ProductResponse::from).toList();
    }

    private void assertOwner(Product product, User seller) {
        if (seller.getRole().name().equals("ADMIN")) {
            return;
        }
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw ApiException.forbidden("You do not own this product");
        }
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> ApiException.notFound("Product not found: " + id));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
