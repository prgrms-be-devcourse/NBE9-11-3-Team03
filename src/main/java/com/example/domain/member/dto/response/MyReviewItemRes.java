package com.example.domain.member.dto.response;

import com.example.domain.review.entity.Review;

import java.time.LocalDateTime;

public record MyReviewItemRes(
        Long reviewId,
        Long festivalId,
        String festivalTitle,
        Integer rating,
        String content,
        String reviewImageUrl,
        Integer likeCount,
        LocalDateTime createdAt
) {
    public static MyReviewItemRes from(Review review){
        return new MyReviewItemRes(
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
