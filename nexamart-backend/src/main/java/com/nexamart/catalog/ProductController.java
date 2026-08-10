package com.nexamart.catalog;

import com.nexamart.auth.User;
import com.nexamart.catalog.dto.ProductRequest;
import com.nexamart.catalog.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public List<ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.search(q, category, minPrice, maxPrice);
    }

    @GetMapping("/api/products/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping("/api/seller/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req,
                                                   @AuthenticationPrincipal User seller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req, seller));
    }

    @GetMapping("/api/seller/products")
    public List<ProductResponse> myProducts(@AuthenticationPrincipal User seller) {
        return productService.listBySeller(seller.getId());
    }

    @PutMapping("/api/seller/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req,
                                   @AuthenticationPrincipal User seller) {
        return productService.update(id, req, seller);
    }

    @DeleteMapping("/api/seller/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User seller) {
        productService.delete(id, seller);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/seller/products/low-stock")
    public List<ProductResponse> lowStock(@RequestParam(defaultValue = "10") int threshold,
                                           @AuthenticationPrincipal User seller) {
        return productService.lowStock(seller.getId(), threshold);
    }
}
