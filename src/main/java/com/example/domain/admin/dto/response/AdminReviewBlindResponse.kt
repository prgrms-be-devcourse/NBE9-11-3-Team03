package com.example.domain.admin.dto.response

import com.example.domain.review.entity.ReviewStatus

@JvmRecord
data class AdminReviewBlindResponse(
    val reviewId: Long,
    val status: ReviewStatus,
    val reportCount: Int
)
