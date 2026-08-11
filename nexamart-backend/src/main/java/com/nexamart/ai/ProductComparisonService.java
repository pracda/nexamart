package com.nexamart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.ai.dto.CompareProductsRequest;
import com.nexamart.ai.dto.ComparisonResult;
import com.nexamart.catalog.ProductService;
import com.nexamart.catalog.dto.ProductResponse;
import com.nexamart.common.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductComparisonService {

    private static final String SYSTEM_PROMPT = """
            You are the NexaMart AI Product Comparison assistant. A buyer is comparing 2-3 real products,
            whose exact title, price, category, description, and stock are given to you below — never invent
            specs beyond what's given. Respond with ONLY a JSON object, no prose, matching exactly:
            {
              "summary": "2-3 sentence plain-English comparison of the key differences",
              "recommendation": "1-2 sentences on which product suits which kind of buyer, or 'it depends' framed helpfully",
              "highlights": [
                { "productId": <id>, "title": "<title>", "pros": ["short pro", "..."], "cons": ["short con", "..."] }
              ]
            }
            Include one highlights entry per product, in the same order given, using the exact productId provided.
            """;

    private final OpenAiClient openAiClient;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public ProductComparisonService(OpenAiClient openAiClient, ProductService productService, ObjectMapper objectMapper) {
        this.openAiClient = openAiClient;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    public ComparisonResult compare(CompareProductsRequest req) {
        List<ProductResponse> products = new ArrayList<>();
        for (Long id : req.productIds()) {
            products.add(productService.getById(id));
        }

        StringBuilder userPrompt = new StringBuilder("Products to compare:\n");
        for (ProductResponse p : products) {
            userPrompt.append("- id: ").append(p.id())
                    .append(", title: ").append(p.title())
                    .append(", price: $").append(p.price())
                    .append(", category: ").append(p.category())
                    .append(", stock: ").append(p.stockQuantity())
                    .append(", description: ").append(p.description())
                    .append("\n");
        }

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt.toString())
        );

        JsonNode response = openAiClient.jsonCompletion(messages);
        String content = response.path("choices").path(0).path("message").path("content").asText("");

        try {
            return objectMapper.readValue(content, ComparisonResult.class);
        } catch (Exception e) {
            throw ApiException.badRequest("The AI assistant returned an unexpected response. Please try again.");
        }
    }
}
