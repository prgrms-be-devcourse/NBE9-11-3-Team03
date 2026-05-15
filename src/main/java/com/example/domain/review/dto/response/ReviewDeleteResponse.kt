package com.example.domain.review.dto.response

import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus

data class ReviewDeleteResponse(
    val reviewId: Long?,
    val status: ReviewStatus?
) {
    companion object {
        @JvmStatic
        fun from(review: Review): ReviewDeleteResponse {
            return ReviewDeleteResponse(
                reviewId = review.id,
                status = review.status
            )
        }
    }
}