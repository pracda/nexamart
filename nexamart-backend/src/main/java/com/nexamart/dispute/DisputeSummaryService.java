package com.nexamart.dispute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.ai.OpenAiClient;
import com.nexamart.common.ApiException;
import com.nexamart.dispute.dto.DisputeSummary;
import com.nexamart.order.Order;
import com.nexamart.order.OrderItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DisputeSummaryService {

    private static final String SYSTEM_PROMPT = """
            You are the NexaMart AI Dispute Summarizer, helping a marketplace admin quickly understand a
            buyer-seller dispute. You are given the order details and the full message thread. Respond with
            ONLY a JSON object, no prose, matching exactly:
            {
              "summary": "exactly 3 neutral, factual sentences summarizing what happened, from both sides",
              "recommendedResolution": "1-2 sentences recommending a fair next step (e.g. refund, replacement, more info needed)"
            }
            Stay neutral - do not take the buyer's or seller's side. Base the summary only on what's in the thread.
            """;

    private final DisputeService disputeService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public DisputeSummaryService(DisputeService disputeService, OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.disputeService = disputeService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public DisputeSummary summarize(Long disputeId) {
        Dispute dispute = disputeService.findOrThrow(disputeId);
        Order order = dispute.getOrder();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Order #").append(order.getId())
                .append(", total $").append(order.getTotalAmount())
                .append(", status ").append(order.getStatus()).append("\n");
        prompt.append("Items: ");
        for (OrderItem item : order.getItems()) {
            prompt.append(item.getProductTitle()).append(" x").append(item.getQuantity()).append("; ");
        }
        prompt.append("\n\nDispute reason: ").append(dispute.getReason()).append("\n\n");
        prompt.append("Message thread:\n");
        List<DisputeMessage> messages = dispute.getMessages();
        for (DisputeMessage m : messages) {
            prompt.append("[").append(m.getAuthor().getRole()).append(" - ").append(m.getAuthor().getFullName()).append("]: ")
                    .append(m.getBody()).append("\n");
        }

        List<Map<String, Object>> chatMessages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt.toString())
        );

        JsonNode response = openAiClient.jsonCompletion(chatMessages);
        String content = response.path("choices").path(0).path("message").path("content").asText("");

        try {
            return objectMapper.readValue(content, DisputeSummary.class);
        } catch (Exception e) {
            throw ApiException.badRequest("The AI assistant returned an unexpected response. Please try again.");
        }
    }
}
