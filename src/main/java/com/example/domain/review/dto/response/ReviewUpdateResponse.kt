package com.example.domain.review.dto.response;

import com.example.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewUpdateResponse {

    private Long reviewId;
    private Long festivalId;
    private Integer rating;
    private String content;
    private String image;
    private LocalDateTime updatedAt;

    public static ReviewUpdateResponse from(Review review) {
        return ReviewUpdateResponse.builder()
                .reviewId(review.getId())
                .festivalId(review.getFestival().getId())
                .rating(review.getRating())
                .content(review.getContent())
                .image(review.getImage())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
