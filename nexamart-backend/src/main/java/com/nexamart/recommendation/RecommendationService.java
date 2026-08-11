package com.nexamart.recommendation;

import com.nexamart.catalog.Product;
import com.nexamart.catalog.ProductService;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.order.OrderItem;
import com.nexamart.order.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Heuristic recommender: category affinity from the buyer's own purchase history,
 * topped up with platform best-sellers and finally newest listings. Not a trained
 * model — a defensible content/collaborative-filtering-lite substitute for the
 * PyTorch service the vision doc describes, scoped to what a course timeline allows.
 */
@Service
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    public RecommendationService(OrderItemRepository orderItemRepository, ProductService productService) {
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
    }

    public List<ProductResponse> recommendForBuyer(Long buyerId, int limit) {
        List<OrderItem> pastItems = orderItemRepository.findByOrder_Buyer_Id(buyerId);
        Set<Long> purchased = pastItems.stream().map(i -> i.getProduct().getId()).collect(Collectors.toSet());

        List<ProductResponse> recs = new ArrayList<>();

        if (!pastItems.isEmpty()) {
            List<String> rankedCategories = pastItems.stream()
                    .collect(Collectors.groupingBy(i -> i.getProduct().getCategory().getName(), Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();

            for (String category : rankedCategories) {
                if (recs.size() >= limit) break;
                for (ProductResponse p : productService.search(null, category, null, null)) {
                    if (recs.size() >= limit) break;
                    if (!purchased.contains(p.id()) && recs.stream().noneMatch(r -> r.id().equals(p.id()))) {
                        recs.add(p);
                    }
                }
            }
        }

        return topUp(recs, limit, purchased);
    }

    private List<ProductResponse> topUp(List<ProductResponse> recs, int limit, Set<Long> exclude) {
        if (recs.size() < limit) {
            Map<Long, Integer> unitsSold = new HashMap<>();
            Map<Long, Product> productsById = new HashMap<>();
            for (OrderItem item : orderItemRepository.findAll()) {
                Long pid = item.getProduct().getId();
                unitsSold.merge(pid, item.getQuantity(), Integer::sum);
                productsById.putIfAbsent(pid, item.getProduct());
            }
            List<Long> ranked = unitsSold.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();

            for (Long id : ranked) {
                if (recs.size() >= limit) break;
                if (exclude.contains(id) || recs.stream().anyMatch(r -> r.id().equals(id))) continue;
                recs.add(ProductResponse.from(productsById.get(id)));
            }
        }

        if (recs.size() < limit) {
            for (ProductResponse p : productService.search(null, null, null, null)) {
                if (recs.size() >= limit) break;
                if (!exclude.contains(p.id()) && recs.stream().noneMatch(r -> r.id().equals(p.id()))) {
                    recs.add(p);
                }
            }
        }

        return recs;
    }
}
