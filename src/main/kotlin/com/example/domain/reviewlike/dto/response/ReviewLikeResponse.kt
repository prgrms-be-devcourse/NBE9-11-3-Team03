package com.example.domain.reviewlike.dto.response

data class ReviewLikeResponse(
    val reviewId: Long,
    val memberId: Long,
    val isLiked: Boolean,
    val likeCount: Int
)