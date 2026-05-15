package com.example.domain.review.dto.response

import com.example.domain.review.entity.Review
import java.time.LocalDateTime

data class ReviewResponse(
    val reviewId: Long?,
    val festivalId: Long?,
    val memberId: Long?,
    val nickname: String?,
    val content: String?,
    val image: String?,
    val rating: Int?,
    val likeCount: Int?,
    val reportCount: Int?,
    val status: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    constructor(review: Review) : this(
        reviewId = review.id,
        festivalId = review.festival.id,
        memberId = review.member.id,
        nickname = review.member.nickname,
        content = review.content,
        image = review.image,
        rating = review.rating,
        likeCount = review.likeCount,
        reportCount = review.reportCount,
        status = review.status.name,
        createdAt = review.createdAt,
        updatedAt = review.updatedAt
    )
}