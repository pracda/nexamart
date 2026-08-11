package com.nexamart.ai.dto;

import java.util.List;

public record ComparisonResult(
        String summary,
        String recommendation,
        List<ProductHighlight> highlights
) {
    public record ProductHighlight(Long productId, String title, List<String> pros, List<String> cons) {
    }
}
