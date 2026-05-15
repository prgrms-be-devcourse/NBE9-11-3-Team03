package com.example.domain.review.dto.response

import com.example.domain.member.entity.MemberStatus
import com.example.domain.review.entity.Review
import java.time.LocalDateTime

data class ReviewListResponse(
    val reviewId: Long,
    val memberId: Long,
    val festivalId: Long,
    val nickname: String,
    val content: String,
    val rating: Int,
    val image: String?,
    val likeCount: Int,
    val reportCount: Int,
    val createdAt: LocalDateTime?,
    val liked: Boolean
) {
    companion object {
        @JvmStatic
        fun from(review: Review, liked: Boolean): ReviewListResponse {
            val member = review.member

            val displayName = if (member.status == MemberStatus.WITHDRAWN) {
                "탈퇴된 회원입니다."
            } else {
                member.nickname
            }

            return ReviewListResponse(
                reviewId = review.id,
                memberId = member.id,
                festivalId = review.festival.id,
                nickname = displayName,
                content = review.content,
                image = review.image,
                rating = review.rating,
                likeCount = review.likeCount,
                reportCount = review.reportCount,
                createdAt = review.createdAt,
                liked = liked
            )
        }
    }
}