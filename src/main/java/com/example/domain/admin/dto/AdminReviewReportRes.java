package com.example.domain.admin.dto;

import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;

import java.time.LocalDateTime;

public record AdminReviewReportRes(
        Long reviewId,
        Long festivalId,
        Long memberId,
        String authorNickname,
        String content,
        Integer reportCount,
        LocalDateTime createdAt,
        ReviewStatus status
) {
    public static AdminReviewReportRes from(Review review){
        return new AdminReviewReportRes (
                review.getId(),
                review.getFestival().getId(),
                review.getMember().getId(),
                review.getMember().getNickname(),
                review.getContent(),
                review.getReportCount(),
                review.getCreatedAt(),
                review.getStatus()
        );
    }
}
