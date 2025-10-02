package com.codelab.micproject.review.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateReview(
        @NotNull Long consultantId,
        @Min(1) @Max(5) int rating,
        @Size(max = 1000) String comment,
        @Size(max = 3) List<@Size(min = 1, max = 20) String> tags
) {}