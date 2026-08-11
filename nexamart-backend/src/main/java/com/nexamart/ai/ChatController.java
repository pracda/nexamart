package com.nexamart.ai;

import com.nexamart.ai.dto.ChatRequest;
import com.nexamart.ai.dto.ChatResponse;
import com.nexamart.ai.dto.CompareProductsRequest;
import com.nexamart.ai.dto.ComparisonResult;
import com.nexamart.ai.dto.GenerateListingRequest;
import com.nexamart.ai.dto.GeneratedListing;
import com.nexamart.ai.dto.PricingAdvice;
import com.nexamart.ai.dto.PricingAdviceRequest;
import com.nexamart.auth.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class ChatController {

    private final AiChatService aiChatService;
    private final ListingAssistantService listingAssistantService;
    private final ProductComparisonService productComparisonService;
    private final PricingAdvisorService pricingAdvisorService;

    public ChatController(AiChatService aiChatService, ListingAssistantService listingAssistantService,
                           ProductComparisonService productComparisonService, PricingAdvisorService pricingAdvisorService) {
        this.aiChatService = aiChatService;
        this.listingAssistantService = listingAssistantService;
        this.productComparisonService = productComparisonService;
        this.pricingAdvisorService = pricingAdvisorService;
    }

    @PostMapping("/api/ai/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, @AuthenticationPrincipal User user) {
        return new ChatResponse(aiChatService.chat(req.message(), user));
    }

    @PostMapping("/api/ai/compare-products")
    public ComparisonResult compareProducts(@Valid @RequestBody CompareProductsRequest req) {
        return productComparisonService.compare(req);
    }

    @PostMapping("/api/seller/ai/generate-listing")
    public GeneratedListing generateListing(@Valid @RequestBody GenerateListingRequest req) {
        return listingAssistantService.generate(req);
    }

    @PostMapping("/api/seller/ai/pricing-advice")
    public PricingAdvice pricingAdvice(@Valid @RequestBody PricingAdviceRequest req) {
        return pricingAdvisorService.advise(req);
    }
}
