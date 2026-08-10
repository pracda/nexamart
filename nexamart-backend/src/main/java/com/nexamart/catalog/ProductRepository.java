package com.nexamart.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySeller_Id(Long sellerId);

    @Query("""
            select p from Product p
            where (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.description) like lower(concat('%', :keyword, '%')))
              and (:category is null or lower(p.category.name) = lower(:category))
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
            order by p.createdAt desc
            """)
    List<Product> search(@Param("keyword") String keyword,
                          @Param("category") String category,
                          @Param("minPrice") BigDecimal minPrice,
                          @Param("maxPrice") BigDecimal maxPrice);

    List<Product> findByStockQuantityLessThan(Integer threshold);
}
