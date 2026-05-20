package com.example.domain.member.dto.response

import com.example.domain.review.entity.Review
import org.springframework.data.domain.Page

data class MyReviewPageResponse(
    val content: List<MyReviewItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
) {
    companion object {
        fun from(reviewPage: Page<Review>) = MyReviewPageResponse(
            content       = reviewPage.content.map(MyReviewItemResponse::from),
            page          = reviewPage.number,
            size          = reviewPage.size,
            totalElements = reviewPage.totalElements,
            totalPages    = reviewPage.totalPages,
            hasNext       = reviewPage.hasNext()
        )
    }
}
