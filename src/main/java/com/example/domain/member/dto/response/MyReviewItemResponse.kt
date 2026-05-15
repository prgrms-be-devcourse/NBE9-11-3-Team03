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
        @JvmStatic
        fun from(review: Review): MyReviewItemResponse {
            return MyReviewItemResponse(
                review.getId(),
                review.getFestival().getId(),
                review.getFestival().getTitle(),
                review.getRating(),
                review.getContent(),
                review.getImage(),
                review.getLikeCount(),
                review.getCreatedAt()
            )
        }
    }
}
