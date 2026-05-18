package com.example.domain.reviewlike.entity

import com.example.domain.member.entity.Member
import com.example.domain.review.entity.Review
import com.example.global.entity.BaseCreatedEntity
import jakarta.persistence.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(
        name = "uk_review_like_member_review",
        columnNames = ["member_id", "review_id"]
    )]
)
class ReviewLike(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    val review: Review
) : BaseCreatedEntity()