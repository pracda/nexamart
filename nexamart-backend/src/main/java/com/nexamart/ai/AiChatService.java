package com.nexamart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.auth.User;
import com.nexamart.common.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private static final int MAX_TOOL_ROUNDS = 4;

    private final OpenAiClient openAiClient;
    private final ToolFunctions toolFunctions;
    private final ObjectMapper objectMapper;

    public AiChatService(OpenAiClient openAiClient, ToolFunctions toolFunctions, ObjectMapper objectMapper) {
        this.openAiClient = openAiClient;
        this.toolFunctions = toolFunctions;
        this.objectMapper = objectMapper;
    }

    public String chat(String userMessage, User currentUser) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt(currentUser)));
        messages.add(Map.of("role", "user", "content", userMessage));

        List<Map<String, Object>> tools = toolFunctions.toolDefinitions(currentUser.getRole());

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JsonNode response = openAiClient.chatCompletion(messages, tools);
            JsonNode choice = response.path("choices").path(0).path("message");

            JsonNode toolCalls = choice.path("tool_calls");
            if (!toolCalls.isMissingNode() && toolCalls.isArray() && !toolCalls.isEmpty()) {
                messages.add(objectMapper.convertValue(choice, Map.class));

                for (JsonNode toolCall : toolCalls) {
                    String callId = toolCall.path("id").asText();
                    String fnName = toolCall.path("function").path("name").asText();
                    String argsJson = toolCall.path("function").path("arguments").asText("{}");

                    JsonNode args;
                    try {
                        args = argsJson.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(argsJson);
                    } catch (Exception e) {
                        args = objectMapper.createObjectNode();
                    }

                    Object result = toolFunctions.execute(fnName, args, currentUser);
                    String resultJson;
                    try {
                        resultJson = objectMapper.writeValueAsString(result);
                    } catch (Exception e) {
                        resultJson = "{\"error\":\"failed to serialize result\"}";
                    }

                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", callId,
                            "content", resultJson
                    ));
                }
                continue;
            }

            String content = choice.path("content").asText("");
            if (content.isBlank()) {
                content = "Sorry, I couldn't come up with a response. Could you rephrase that?";
            }
            return content;
        }

        throw ApiException.badRequest("The AI assistant took too many steps to answer. Please try a simpler question.");
    }

    private String systemPrompt(User user) {
        String roleGuidance = switch (user.getRole()) {
            case BUYER -> """
                    - Help the buyer find products (use search_products) and check their own orders (use get_order_status).
                    - Only call get_order_status for orders belonging to the current user; if a lookup fails, say so politely.
                    - Summarize search results as a short list with price and category.""";
            case SELLER -> """
                    - Help the seller understand their own inventory (use list_my_products, get_low_stock_products)
                      and their sales performance (use get_seller_sales_summary).
                    - These tools are already scoped to this seller's own products and orders — never ask which seller, just call them.
                    - When reporting sales figures, state the period covered and round currency to 2 decimals.""";
            case ADMIN -> """
                    - You can look up any buyer's product search results, any order by id, and any seller's inventory or sales
                      (the seller tools operate on this admin account's own listings only, which may be empty — say so if it is).""";
            default -> "- No specialized tools are available for this role yet.";
        };

        return """
                You are the NexaMart marketplace assistant. The current user is "%s" with role %s.
                %s
                - Keep replies concise, friendly, and in plain English.
                - If you don't have enough information to call a function, ask a brief clarifying question instead of guessing.
                - Never invent product, order, inventory, or sales data that wasn't returned by a function call.
                """.formatted(user.getFullName(), user.getRole(), roleGuidance);
    }
}
