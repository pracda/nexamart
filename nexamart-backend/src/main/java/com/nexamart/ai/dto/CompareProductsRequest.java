package com.nexamart.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompareProductsRequest(
        @NotNull @Size(min = 2, max = 3, message = "compare 2 to 3 products at a time") List<Long> productIds
) {
}
