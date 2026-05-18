package com.example.domain.reviewlike.repository

import com.example.domain.reviewlike.entity.ReviewLike
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ReviewLikeRepository : JpaRepository<ReviewLike, Long> {
    fun existsByMemberIdAndReviewId(memberId: Long, reviewId: Long): Boolean
    fun findByMemberIdAndReviewId(memberId: Long, reviewId: Long): ReviewLike?
}