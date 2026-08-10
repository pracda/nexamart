package com.nexamart.ai;

import com.nexamart.ai.dto.ChatRequest;
import com.nexamart.ai.dto.ChatResponse;
import com.nexamart.ai.dto.GenerateListingRequest;
import com.nexamart.ai.dto.GeneratedListing;
import com.nexamart.auth.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class ChatController {

    private final AiChatService aiChatService;
    private final ListingAssistantService listingAssistantService;

    public ChatController(AiChatService aiChatService, ListingAssistantService listingAssistantService) {
        this.aiChatService = aiChatService;
        this.listingAssistantService = listingAssistantService;
    }

    @PostMapping("/api/ai/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, @AuthenticationPrincipal User user) {
        return new ChatResponse(aiChatService.chat(req.message(), user));
    }

    @PostMapping("/api/seller/ai/generate-listing")
    public GeneratedListing generateListing(@Valid @RequestBody GenerateListingRequest req) {
        return listingAssistantService.generate(req);
    }
}
