package com.example.domain.admin.dto;

import com.example.domain.review.entity.ReviewStatus;

public record AdminReviewBlindRes(
        Long reviewId,
        ReviewStatus status,
        Integer reportCount
) {
}
