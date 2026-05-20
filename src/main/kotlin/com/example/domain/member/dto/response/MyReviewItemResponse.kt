package com.example.domain.member.dto.response

import com.example.domain.review.entity.Review
import java.time.LocalDateTime


data class MyReviewItemResponse(
    val reviewId: Long,
    val festivalId: Long,
    val festivalTitle: String,
    val rating: Int,

    val content: String,
    val reviewImageUrl: String?,
    val likeCount: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(review: Review): MyReviewItemResponse {
            return MyReviewItemResponse(
                review.id,
                review.festival.id,
                review.festival.title,
                review.rating,
                review.content,
                review.image,
                review.likeCount,
                review.createdAt
            )
        }
    }
}
