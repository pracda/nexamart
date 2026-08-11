package com.nexamart.dispute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    List<Dispute> findByOpenedBy_IdOrderByCreatedAtDesc(Long buyerId);

    @Query("""
            select distinct d from Dispute d
            join d.order.items i
            where i.product.seller.id = :sellerId
            order by d.createdAt desc
            """)
    List<Dispute> findForSeller(@Param("sellerId") Long sellerId);

    List<Dispute> findAllByOrderByCreatedAtDesc();
}
