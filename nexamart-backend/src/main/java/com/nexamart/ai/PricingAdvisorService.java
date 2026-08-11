package com.nexamart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.ai.dto.PricingAdvice;
import com.nexamart.ai.dto.PricingAdviceRequest;
import com.nexamart.catalog.ProductService;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.common.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PricingAdvisorService {

    private static final String SYSTEM_PROMPT = """
            You are the NexaMart AI Pricing Advisor. A seller is about to list a new product and wants a
            competitive price range. You are given the product's working title/category and, when available,
            real prices of similar products already on the platform. Respond with ONLY a JSON object, no prose:
            {
              "recommendedMin": <number>,
              "recommendedMax": <number>,
              "justification": "1-3 sentences explaining the range, referencing the comparable prices if given"
            }
            If no comparable products were provided, say so plainly in the justification and give a reasonable
            estimate based on general knowledge of the category, clearly caveated as a rough estimate.
            """;

    private final OpenAiClient openAiClient;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public PricingAdvisorService(OpenAiClient openAiClient, ProductService productService, ObjectMapper objectMapper) {
        this.openAiClient = openAiClient;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    public PricingAdvice advise(PricingAdviceRequest req) {
        List<ProductResponse> comparable = productService.search(null, req.category(), null, null);

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("New listing working title: ").append(req.title()).append("\n");
        userPrompt.append("Category: ").append(req.category()).append("\n");
        if (req.draftPrice() != null) {
            userPrompt.append("Seller's draft price idea: $").append(req.draftPrice()).append("\n");
        }

        if (comparable.isEmpty()) {
            userPrompt.append("No comparable products currently listed in this category on the platform.\n");
        } else {
            BigDecimal min = comparable.stream().map(ProductResponse::price).min(BigDecimal::compareTo).orElseThrow();
            BigDecimal max = comparable.stream().map(ProductResponse::price).max(BigDecimal::compareTo).orElseThrow();
            BigDecimal avg = comparable.stream().map(ProductResponse::price).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(comparable.size()), 2, java.math.RoundingMode.HALF_UP);
            userPrompt.append("Comparable products in this category (").append(comparable.size()).append(" total): ")
                    .append("min $").append(min).append(", max $").append(max).append(", avg $").append(avg).append("\n");
            userPrompt.append("Sample listings: ");
            comparable.stream().limit(6).forEach(p ->
                    userPrompt.append(p.title()).append(" ($").append(p.price()).append("), "));
            userPrompt.append("\n");
        }

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt.toString())
        );

        JsonNode response = openAiClient.jsonCompletion(messages);
        String content = response.path("choices").path(0).path("message").path("content").asText("");

        try {
            return objectMapper.readValue(content, PricingAdvice.class);
        } catch (Exception e) {
            throw ApiException.badRequest("The AI assistant returned an unexpected response. Please try again.");
        }
    }
}
