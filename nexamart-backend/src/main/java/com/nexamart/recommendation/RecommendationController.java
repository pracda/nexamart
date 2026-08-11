package com.nexamart.recommendation;

import com.nexamart.auth.User;
import com.nexamart.catalog.dto.ProductResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/api/recommendations")
    public List<ProductResponse> recommend(@RequestParam(defaultValue = "6") int limit,
                                            @AuthenticationPrincipal User user) {
        return recommendationService.recommendForBuyer(user.getId(), Math.min(limit, 12));
    }
}
