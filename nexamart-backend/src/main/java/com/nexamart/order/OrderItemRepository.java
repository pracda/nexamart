package com.nexamart.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select oi from OrderItem oi
            where oi.product.seller.id = :sellerId
              and (:from is null or oi.order.createdAt >= :from)
              and (:to is null or oi.order.createdAt <= :to)
            """)
    List<OrderItem> findForSeller(@Param("sellerId") Long sellerId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    List<OrderItem> findByOrder_Buyer_Id(Long buyerId);

    @Query("""
            select oi from OrderItem oi
            where (:from is null or oi.order.createdAt >= :from)
              and (:to is null or oi.order.createdAt <= :to)
            """)
    List<OrderItem> findAllInRange(@Param("from") Instant from, @Param("to") Instant to);
}
