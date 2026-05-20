package com.example.domain.review.entity

import com.example.domain.festival.entity.Festival
import com.example.domain.member.entity.Member
import com.example.global.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Entity
@Table(name = "review")
class Review(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    val festival: Festival,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    var image: String?,

    @field:Min(value = 1, message = "평점은 최소 1점입니다.")
    @field:Max(value = 5, message = "평점은 최대 5점입니다.")
    @Column(nullable = false)
    var rating: Int

) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReviewStatus = ReviewStatus.ACTIVE
        protected set

    @Column(nullable = false)
    var likeCount: Int = 0
        protected set

    @Column(nullable = false)
    var reportCount: Int = 0
        protected set

    fun updateReview(content: String, image: String?, rating: Int) {
        this.content = content
        this.image = image
        this.rating = rating
    }

    fun deleteReview() {
        this.status = ReviewStatus.DELETED
    }

    fun reviewBlind() {
        this.status = ReviewStatus.BLIND
    }

    fun reportCountReset() {
        this.reportCount = 0
    }

    // 리뷰가 신고될 때마다 누적 신고 수를 1 증가시킵니다.
    fun increaseReportCount() {
        this.reportCount++
    }

    //리뷰 좋아요 카운트 및 좋아요 취소
    fun increaseLikeCount() {
        this.likeCount++
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }
}