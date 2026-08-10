package com.nexamart.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId);
    Optional<Order> findByIdAndBuyer_Id(Long id, Long buyerId);
}
