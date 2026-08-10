package com.nexamart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexamart.analytics.SellerAnalyticsService;
import com.nexamart.analytics.dto.SellerSalesSummary;
import com.nexamart.auth.Role;
import com.nexamart.auth.User;
import com.nexamart.catalog.ProductService;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.common.ApiException;
import com.nexamart.order.OrderService;
import com.nexamart.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ToolFunctions {

    private final ProductService productService;
    private final OrderService orderService;
    private final SellerAnalyticsService sellerAnalyticsService;

    public ToolFunctions(ProductService productService, OrderService orderService,
                          SellerAnalyticsService sellerAnalyticsService) {
        this.productService = productService;
        this.orderService = orderService;
        this.sellerAnalyticsService = sellerAnalyticsService;
    }

    /** Returns the tool definitions available to this user's role. */
    public List<Map<String, Object>> toolDefinitions(Role role) {
        List<Map<String, Object>> tools = new ArrayList<>();

        if (role == Role.BUYER || role == Role.ADMIN) {
            tools.add(tool("search_products",
                    "Search the NexaMart product catalog by keyword, category, and price range. Use this whenever the buyer describes what they want to find or buy.",
                    Map.of(
                            "keyword", param("string", "Free-text search term, e.g. product name or type"),
                            "category", param("string", "Category name, e.g. Electronics, Fashion, Home & Kitchen"),
                            "minPrice", param("number", "Minimum price in USD"),
                            "maxPrice", param("number", "Maximum price in USD")
                    ),
                    List.of()));

            tools.add(tool("get_order_status",
                    "Look up the status, items, and estimated delivery of one of the current buyer's own orders by order id.",
                    Map.of("orderId", param("integer", "The numeric order id, e.g. 1023")),
                    List.of("orderId")));
        }

        if (role == Role.SELLER || role == Role.ADMIN) {
            tools.add(tool("list_my_products",
                    "List the current seller's own product listings, optionally filtered by a keyword. Use for questions like 'what do I have in electronics' or 'show my listings'.",
                    Map.of("keyword", param("string", "Optional keyword to filter the seller's own products by title")),
                    List.of()));

            tools.add(tool("get_low_stock_products",
                    "List the current seller's products whose stock quantity is below a threshold. Use for questions like 'which products are running low' or 'what needs restocking'.",
                    Map.of("threshold", param("integer", "Stock quantity threshold, default 10 if not specified")),
                    List.of()));

            tools.add(tool("get_seller_sales_summary",
                    "Get the current seller's sales performance: total revenue, units sold, order count, and top-selling products, optionally within a date range (YYYY-MM-DD). Use for questions like 'what were my top selling products' or 'how much revenue did I make this month'.",
                    Map.of(
                            "fromDate", param("string", "Start date in YYYY-MM-DD format, omit for all time"),
                            "toDate", param("string", "End date in YYYY-MM-DD format, omit for up to now"),
                            "topN", param("integer", "How many top products to return, default 5")
                    ),
                    List.of()));
        }

        return tools;
    }

    public Object execute(String name, JsonNode arguments, User currentUser) {
        return switch (name) {
            case "search_products" -> searchProducts(arguments);
            case "get_order_status" -> getOrderStatus(arguments, currentUser);
            case "list_my_products" -> listMyProducts(arguments, currentUser);
            case "get_low_stock_products" -> getLowStockProducts(arguments, currentUser);
            case "get_seller_sales_summary" -> getSellerSalesSummary(arguments, currentUser);
            default -> Map.of("error", "Unknown function: " + name);
        };
    }

    private List<ProductResponse> searchProducts(JsonNode args) {
        String keyword = textOrNull(args, "keyword");
        String category = textOrNull(args, "category");
        BigDecimal minPrice = numberOrNull(args, "minPrice");
        BigDecimal maxPrice = numberOrNull(args, "maxPrice");
        return productService.search(keyword, category, minPrice, maxPrice).stream().limit(8).toList();
    }

    private Object getOrderStatus(JsonNode args, User currentUser) {
        if (!args.hasNonNull("orderId")) {
            return Map.of("error", "orderId is required");
        }
        Long orderId = args.get("orderId").asLong();
        try {
            OrderResponse order = currentUser.getRole() == Role.BUYER
                    ? orderService.getForBuyer(orderId, currentUser.getId())
                    : orderService.getById(orderId);
            return order;
        } catch (ApiException e) {
            return Map.of("error", e.getMessage());
        }
    }

    private List<ProductResponse> listMyProducts(JsonNode args, User currentUser) {
        String keyword = textOrNull(args, "keyword");
        List<ProductResponse> mine = productService.listBySeller(currentUser.getId());
        if (keyword == null) {
            return mine.stream().limit(15).toList();
        }
        String needle = keyword.toLowerCase();
        return mine.stream().filter(p -> p.title().toLowerCase().contains(needle)).limit(15).toList();
    }

    private List<ProductResponse> getLowStockProducts(JsonNode args, User currentUser) {
        int threshold = args.hasNonNull("threshold") ? args.get("threshold").asInt() : 10;
        return productService.lowStock(currentUser.getId(), threshold);
    }

    private SellerSalesSummary getSellerSalesSummary(JsonNode args, User currentUser) {
        Instant from = dateOrNull(args, "fromDate");
        Instant to = dateOrNull(args, "toDate");
        int topN = args.hasNonNull("topN") ? args.get("topN").asInt() : 5;
        return sellerAnalyticsService.salesSummary(currentUser.getId(), from, to, topN);
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", Map.of(
                                "type", "object",
                                "properties", properties,
                                "required", required
                        )
                )
        );
    }

    private Map<String, Object> param(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private String textOrNull(JsonNode node, String field) {
        return (node.hasNonNull(field) && !node.get(field).asText().isBlank()) ? node.get(field).asText() : null;
    }

    private BigDecimal numberOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? BigDecimal.valueOf(node.get(field).asDouble()) : null;
    }

    private Instant dateOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field) || node.get(field).asText().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(node.get(field).asText()).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
