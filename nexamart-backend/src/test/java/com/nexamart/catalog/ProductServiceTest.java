package com.nexamart.catalog;

import com.nexamart.auth.Role;
import com.nexamart.auth.User;
import com.nexamart.catalog.dto.ProductRequest;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private User seller;
    private User otherSeller;
    private User admin;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = new User("seller@test.dev", "hash", "Sam Seller", Role.SELLER);
        seller.setId(2L);
        otherSeller = new User("other@test.dev", "hash", "Other Seller", Role.SELLER);
        otherSeller.setId(5L);
        admin = new User("admin@test.dev", "hash", "Nexa Admin", Role.ADMIN);
        admin.setId(1L);
        category = new Category("Electronics");
        category.setId(1L);
    }

    @Test
    void search_withBlankKeywordAndCategory_normalizesToNullBeforeQuerying() {
        when(productRepository.search(null, null, null, null)).thenReturn(List.of());

        List<ProductResponse> result = productService.search("  ", "", null, null);

        assertThat(result).isEmpty();
        verify(productRepository).search(null, null, null, null);
    }

    @Test
    void create_withNewCategoryName_createsCategoryThenSavesProduct() {
        when(categoryRepository.findByNameIgnoreCase("Electronics")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProductRequest req = new ProductRequest("Widget", "desc", new BigDecimal("9.99"), 5, null, "Electronics");
        ProductResponse response = productService.create(req, seller);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo("Electronics");
        assertThat(response.sellerId()).isEqualTo(2L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void update_byNonOwningSeller_throwsForbidden() {
        Product existing = new Product("Widget", "desc", new BigDecimal("9.99"), 5, null, category, seller);
        existing.setId(10L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));

        ProductRequest req = new ProductRequest("New title", "desc", new BigDecimal("9.99"), 5, null, "Electronics");

        ApiException ex = assertThrows(ApiException.class, () -> productService.update(10L, req, otherSeller));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_byAdmin_isAllowedRegardlessOfOwnership() {
        Product existing = new Product("Widget", "desc", new BigDecimal("9.99"), 5, null, category, seller);
        existing.setId(10L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));

        productService.delete(10L, admin);

        verify(productRepository).delete(existing);
    }

    @Test
    void getById_unknownId_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> productService.getById(999L));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
