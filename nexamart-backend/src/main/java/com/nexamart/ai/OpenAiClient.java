package com.nexamart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.common.ApiException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode chatCompletion(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", messages,
                "tools", tools,
                "tool_choice", "auto",
                "temperature", 0.3
        );
        return post(body);
    }

    /** One-shot completion with no tool calling, forcing a JSON object response. */
    public JsonNode jsonCompletion(List<Map<String, Object>> messages) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", messages,
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.5
        );
        return post(body);
    }

    private JsonNode post(Map<String, Object> body) {
        if (!properties.isConfigured()) {
            throw ApiException.badRequest("AI assistant is not configured. Set OPENAI_API_KEY on the server.");
        }

        RestClient client = RestClient.builder().baseUrl(properties.getBaseUrl()).build();

        return client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getOpenaiApiKey())
                .body(body)
                .exchange((request, response) -> {
                    String raw = new String(response.getBody().readAllBytes());
                    if (response.getStatusCode().isError()) {
                        HttpStatusCode status = response.getStatusCode();
                        throw ApiException.badRequest("AI provider error (" + status.value() + "): " + raw);
                    }
                    return objectMapper.readTree(raw);
                });
    }
}
