package com.example.domain.admin.dto.response;

import com.example.domain.review.entity.ReviewStatus;

public record AdminReviewBlindResponse(
        Long reviewId,
        ReviewStatus status,
        Integer reportCount
) {
}
