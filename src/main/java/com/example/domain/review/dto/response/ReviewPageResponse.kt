package com.example.domain.review.dto.response

data class ReviewPageResponse(
    val festivalId: Long,
    val content: List<ReviewListResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)