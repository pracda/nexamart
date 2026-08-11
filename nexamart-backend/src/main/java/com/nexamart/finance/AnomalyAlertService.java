package com.nexamart.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexamart.ai.OpenAiClient;
import com.nexamart.finance.dto.AnomalyAlert;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class AnomalyAlertService {

    private static final String SYSTEM_PROMPT = """
            You are the NexaMart AI Anomaly Alerts assistant for the finance team. You are given this period's
            and the previous period's platform revenue. Respond with ONLY a JSON object:
            { "significant": true or false, "message": "1-3 sentence plain-language alert or reassurance" }
            Flag significant=true only for a genuinely large swing (roughly 40%+ change, or new revenue where
            there was previously none). If the change looks like normal day-to-day variation, say so plainly
            and reassuringly with significant=false — do not manufacture alarm from noise.
            """;

    private record AiAssessment(boolean significant, String message) {
    }

    private final FinanceAnalyticsService financeAnalyticsService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public AnomalyAlertService(FinanceAnalyticsService financeAnalyticsService, OpenAiClient openAiClient,
                                ObjectMapper objectMapper) {
        this.financeAnalyticsService = financeAnalyticsService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public AnomalyAlert checkRevenueAnomaly() {
        Instant now = Instant.now();
        Instant currentStart = now.minus(7, ChronoUnit.DAYS);
        Instant previousStart = now.minus(14, ChronoUnit.DAYS);

        BigDecimal currentRevenue = financeAnalyticsService.totalRevenueInRange(currentStart, now);
        BigDecimal previousRevenue = financeAnalyticsService.totalRevenueInRange(previousStart, currentStart);

        String prompt = "Current period (last 7 days) revenue: $" + currentRevenue +
                "\nPrevious period (7 to 14 days ago) revenue: $" + previousRevenue;

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
        );

        JsonNode response = openAiClient.jsonCompletion(messages);
        String content = response.path("choices").path(0).path("message").path("content").asText("");

        AiAssessment assessment;
        try {
            assessment = objectMapper.readValue(content, AiAssessment.class);
        } catch (Exception e) {
            assessment = new AiAssessment(false, "Could not analyze the trend right now — please try again.");
        }

        return new AnomalyAlert("last 7 days", currentRevenue, "7 to 14 days ago", previousRevenue,
                assessment.significant(), assessment.message());
    }
}
