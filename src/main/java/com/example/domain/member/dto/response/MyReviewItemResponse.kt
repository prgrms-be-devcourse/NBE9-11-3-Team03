package com.example.domain.member.dto.response;

import com.example.domain.review.entity.Review;

import java.time.LocalDateTime;

public record MyReviewItemResponse(
        Long reviewId,
        Long festivalId,
        String festivalTitle,
        Integer rating,
        String content,
        String reviewImageUrl,
        Integer likeCount,
        LocalDateTime createdAt
) {
    public static MyReviewItemResponse from(Review review){
        return new MyReviewItemResponse(
                review.getId(),
                review.getFestival().getId(),
                review.getFestival().getTitle(),
                review.getRating(),
                review.getContent(),
                review.getImage(),
                review.getLikeCount(),
                review.getCreatedAt()
        );
    }
}
