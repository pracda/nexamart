package com.nexamart.order;

import com.nexamart.auth.Role;
import com.nexamart.auth.User;
import com.nexamart.catalog.Category;
import com.nexamart.catalog.Product;
import com.nexamart.catalog.ProductRepository;
import com.nexamart.common.ApiException;
import com.nexamart.order.dto.OrderResponse;
import com.nexamart.order.dto.PlaceOrderRequest;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User buyer;
    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = new User("seller@test.dev", "hash", "Sam Seller", Role.SELLER);
        seller.setId(2L);
        buyer = new User("buyer@test.dev", "hash", "Bailey Buyer", Role.BUYER);
        buyer.setId(3L);
        category = new Category("Electronics");
        category.setId(1L);
    }

    @Test
    void placeOrder_withSufficientStock_decrementsStockAndReturnsOrderTotal() {
        Product product = new Product("Widget", "desc", new BigDecimal("10.00"), 5, null, category, seller);
        product.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        PlaceOrderRequest req = new PlaceOrderRequest(List.of(new PlaceOrderRequest.OrderLine(1L, 2)));

        OrderResponse response = orderService.placeOrder(req, buyer);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(response.items()).hasSize(1);
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void placeOrder_withInsufficientStock_throwsBadRequestAndLeavesStockUnchanged() {
        Product product = new Product("Widget", "desc", new BigDecimal("10.00"), 1, null, category, seller);
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        PlaceOrderRequest req = new PlaceOrderRequest(List.of(new PlaceOrderRequest.OrderLine(1L, 5)));

        ApiException ex = assertThrows(ApiException.class, () -> orderService.placeOrder(req, buyer));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(product.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void placeOrder_withZeroQuantity_throwsBadRequestBeforeTouchingCatalog() {
        PlaceOrderRequest req = new PlaceOrderRequest(List.of(new PlaceOrderRequest.OrderLine(1L, 0)));

        ApiException ex = assertThrows(ApiException.class, () -> orderService.placeOrder(req, buyer));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(productRepository);
    }

    @Test
    void getForBuyer_orderBelongsToDifferentBuyer_throwsNotFound() {
        when(orderRepository.findByIdAndBuyer_Id(100L, 3L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> orderService.getForBuyer(100L, 3L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
