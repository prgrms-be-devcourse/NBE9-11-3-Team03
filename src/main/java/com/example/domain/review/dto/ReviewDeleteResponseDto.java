package com.example.domain.review.dto;

import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewDeleteResponseDto {

    private Long reviewId;
    private ReviewStatus status;

    public static ReviewDeleteResponseDto from(Review review) {
        return new ReviewDeleteResponseDto(
                review.getId(),
                review.getStatus()
        );
    }
}
