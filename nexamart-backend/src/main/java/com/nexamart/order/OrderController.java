package com.nexamart.order;

import com.nexamart.auth.User;
import com.nexamart.order.dto.OrderResponse;
import com.nexamart.order.dto.PlaceOrderRequest;
import com.nexamart.order.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest req,
                                                @AuthenticationPrincipal User buyer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(req, buyer));
    }

    @GetMapping
    public List<OrderResponse> myOrders(@AuthenticationPrincipal User buyer) {
        return orderService.myOrders(buyer.getId());
    }

    @GetMapping("/{id}")
    public OrderResponse getOne(@PathVariable Long id, @AuthenticationPrincipal User buyer) {
        if (buyer.getRole().name().equals("ADMIN") || buyer.getRole().name().equals("SELLER")) {
            return orderService.getById(id);
        }
        return orderService.getForBuyer(id, buyer.getId());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req,
                                       @AuthenticationPrincipal User actor) {
        return orderService.updateStatus(id, req.status());
    }
}
