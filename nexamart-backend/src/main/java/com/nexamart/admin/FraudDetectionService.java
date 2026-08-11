package com.nexamart.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.admin.dto.FraudFlag;
import com.nexamart.admin.dto.FraudScanResult;
import com.nexamart.ai.OpenAiClient;
import com.nexamart.catalog.ProductService;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.order.OrderItem;
import com.nexamart.order.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Heuristic anomaly scan (price outliers within a category, rapid repeat orders of the same
 * product by the same buyer) with AI-written plain-language explanations. Stands in for the
 * vision doc's "continuous review-pattern monitoring" — this app has no review/rating feature
 * yet, so pricing and order-pattern signals are used instead, clearly documented as a simplification.
 */
@Service
public class FraudDetectionService {

    private static final String SYSTEM_PROMPT = """
            You are the NexaMart AI Fraud & Anomaly Detector, helping a marketplace admin triage a short
            list of automatically flagged listings or order patterns. For each numbered candidate given,
            write ONE short, plain-English sentence explaining why it was flagged and how urgently an admin
            should review it. Respond with ONLY a JSON object:
            { "explanations": ["explanation for candidate 1", "explanation for candidate 2", ...] }
            Keep the same order and count as the candidates given. Be calibrated, not alarmist: simple
            statistical rules produce false positives often, so say so plainly when a candidate looks benign
            (e.g. a cheap accessory in an otherwise pricier category is not fraud).
            """;

    private record Candidate(String type, String subject, String evidence) {
    }

    private record Explanations(List<String> explanations) {
    }

    private final ProductService productService;
    private final OrderItemRepository orderItemRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public FraudDetectionService(ProductService productService, OrderItemRepository orderItemRepository,
                                  OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.productService = productService;
        this.orderItemRepository = orderItemRepository;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public FraudScanResult scan() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(findPriceOutliers());
        candidates.addAll(findRepeatOrderPatterns());

        if (candidates.isEmpty()) {
            return new FraudScanResult(List.of(), "No anomalies detected in the current data.");
        }

        List<String> explanations = explainWithAi(candidates);

        List<FraudFlag> flags = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            String explanation = i < explanations.size() ? explanations.get(i) : c.evidence();
            flags.add(new FraudFlag(c.type(), c.subject(), c.evidence(), explanation));
        }
        return new FraudScanResult(flags, flags.size() + " potential anomal" + (flags.size() == 1 ? "y" : "ies") + " flagged for review.");
    }

    private List<Candidate> findPriceOutliers() {
        List<ProductResponse> all = productService.search(null, null, null, null);
        Map<String, List<ProductResponse>> byCategory = all.stream()
                .collect(Collectors.groupingBy(ProductResponse::category));

        List<Candidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<ProductResponse>> entry : byCategory.entrySet()) {
            List<ProductResponse> items = entry.getValue();
            if (items.size() < 2) continue;

            BigDecimal avg = items.stream().map(ProductResponse::price).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(items.size()), 4, RoundingMode.HALF_UP);

            for (ProductResponse p : items) {
                BigDecimal ratio = p.price().divide(avg, 4, RoundingMode.HALF_UP);
                if (ratio.compareTo(new BigDecimal("0.4")) < 0 || ratio.compareTo(new BigDecimal("2.5")) > 0) {
                    candidates.add(new Candidate(
                            "PRICE_OUTLIER",
                            p.title() + " (seller: " + p.sellerName() + ")",
                            "Priced at $" + p.price() + " vs the " + entry.getKey() + " category average of $"
                                    + avg.setScale(2, RoundingMode.HALF_UP)
                    ));
                }
            }
        }
        return candidates;
    }

    private List<Candidate> findRepeatOrderPatterns() {
        List<OrderItem> items = orderItemRepository.findAll();
        Map<String, List<Instant>> timesByBuyerProduct = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();

        for (OrderItem item : items) {
            String key = item.getOrder().getBuyer().getId() + ":" + item.getProduct().getId();
            timesByBuyerProduct.computeIfAbsent(key, k -> new ArrayList<>()).add(item.getOrder().getCreatedAt());
            labels.put(key, item.getOrder().getBuyer().getFullName() + " re-ordering " + item.getProductTitle());
        }

        List<Candidate> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Instant>> entry : timesByBuyerProduct.entrySet()) {
            List<Instant> times = new ArrayList<>(entry.getValue());
            if (times.size() < 2) continue;
            Collections.sort(times);

            for (int i = 1; i < times.size(); i++) {
                Duration gap = Duration.between(times.get(i - 1), times.get(i));
                if (gap.toHours() < 24) {
                    candidates.add(new Candidate(
                            "REPEAT_ORDER_PATTERN",
                            labels.get(entry.getKey()),
                            "Same buyer ordered the same product " + times.size() + " times total, two of them only "
                                    + gap.toMinutes() + " minutes apart"
                    ));
                    break;
                }
            }
        }
        return candidates;
    }

    private List<String> explainWithAi(List<Candidate> candidates) {
        StringBuilder prompt = new StringBuilder("Candidates:\n");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            prompt.append(i + 1).append(". [").append(c.type()).append("] ").append(c.subject())
                    .append(" — ").append(c.evidence()).append("\n");
        }

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt.toString())
        );

        JsonNode response = openAiClient.jsonCompletion(messages);
        String content = response.path("choices").path(0).path("message").path("content").asText("");

        try {
            return objectMapper.readValue(content, Explanations.class).explanations();
        } catch (Exception e) {
            return List.of();
        }
    }
}
