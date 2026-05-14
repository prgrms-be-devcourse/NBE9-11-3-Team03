package com.example.domain.review.dto.response;

import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewDeleteResponse {

    private Long reviewId;
    private ReviewStatus status;

    public static ReviewDeleteResponse from(Review review) {
        return new ReviewDeleteResponse(
                review.getId(),
                review.getStatus()
        );
    }
}
