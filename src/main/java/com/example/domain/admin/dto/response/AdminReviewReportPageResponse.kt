package com.example.domain.admin.dto.response

import com.example.domain.review.entity.Review
import org.springframework.data.domain.Page

data class AdminReviewReportPageResponse(
    val content: List<AdminReviewReportResponse>, // MutableList 대신 List 사용
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        @JvmStatic
        fun from(reviewPage: Page<Review>): AdminReviewReportPageResponse {
            return AdminReviewReportPageResponse(
                // Stream API 대신 코틀린의 내장 map 함수 사용
                content = reviewPage.content.map { AdminReviewReportResponse.from(it) },
                page = reviewPage.number,
                size = reviewPage.size,
                totalElements = reviewPage.totalElements,
                totalPages = reviewPage.totalPages
            )
        }
    }
}