package com.example.domain.admin.dto.response

import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import java.time.LocalDateTime

/**
 * 관리자용 리뷰 신고 응답 DTO
 */
data class AdminReviewReportResponse(
    val reviewId: Long,
    val festivalId: Long,
    val memberId: Long,
    val authorNickname: String,
    val content: String,
    val reportCount: Int,
    val createdAt: LocalDateTime,
    val status: ReviewStatus
) {
    companion object {
        fun from(review: Review): AdminReviewReportResponse {
            return AdminReviewReportResponse(
                reviewId = review.id,
                festivalId = review.festival.id,
                memberId = review.member.id,
                authorNickname = review.member.nickname,
                content = review.content,
                reportCount = review.reportCount,
                createdAt = review.createdAt,
                status = review.status
            )
        }
    }
}