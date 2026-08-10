package com.nexamart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.ai.dto.GenerateListingRequest;
import com.nexamart.ai.dto.GeneratedListing;
import com.nexamart.common.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListingAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the NexaMart AI Listing Assistant. A seller gives you a rough product name and optional notes.
            Generate a polished marketplace listing and respond with ONLY a JSON object, no prose, matching exactly:
            {
              "title": "short compelling product title, under 80 characters",
              "description": "2-4 sentence persuasive product description",
              "bulletFeatures": ["3 to 5 short feature bullets"],
              "seoTags": ["5 to 8 lowercase search keywords/tags"],
              "suggestedCategory": "one of: Electronics, Fashion, Home & Kitchen, or a sensible new category name"
            }
            Never invent specific technical specs (exact battery life, materials, certifications) that weren't provided —
            keep claims generic and marketing-appropriate instead of fabricating facts.
            """;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public ListingAssistantService(OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public GeneratedListing generate(GenerateListingRequest req) {
        String userPrompt = "Product name: " + req.productName() +
                (req.notes() != null && !req.notes().isBlank() ? "\nSeller notes: " + req.notes() : "");

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userPrompt));

        JsonNode response = openAiClient.jsonCompletion(messages);
        String content = response.path("choices").path(0).path("message").path("content").asText("");

        try {
            return objectMapper.readValue(content, GeneratedListing.class);
        } catch (Exception e) {
            throw ApiException.badRequest("The AI assistant returned an unexpected response. Please try again.");
        }
    }
}
