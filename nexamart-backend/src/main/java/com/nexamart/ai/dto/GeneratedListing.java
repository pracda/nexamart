package com.nexamart.ai.dto;

import java.util.List;

public record GeneratedListing(
        String title,
        String description,
        List<String> bulletFeatures,
        List<String> seoTags,
        String suggestedCategory
) {
}
