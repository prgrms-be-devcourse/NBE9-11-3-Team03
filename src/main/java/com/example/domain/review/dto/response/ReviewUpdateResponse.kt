package com.example.domain.review.dto.response

import com.example.domain.review.entity.Review
import java.time.LocalDateTime

data class ReviewUpdateResponse(
    val reviewId: Long?,
    val festivalId: Long?,
    val rating: Int?,
    val content: String?,
    val image: String?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun from(review: Review): ReviewUpdateResponse {
            return ReviewUpdateResponse(
                reviewId = review.id,
                festivalId = review.festival.id,
                rating = review.rating,
                content = review.content,
                image = review.image,
                updatedAt = review.updatedAt
            )
        }
    }
}
