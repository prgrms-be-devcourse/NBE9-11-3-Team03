package com.example.domain.reviewreport.entity

import com.example.domain.member.entity.Member
import com.example.domain.review.entity.Review
import com.example.global.entity.BaseCreatedEntity
import jakarta.persistence.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(
        name = "uk_review_report_reporter_review",
        columnNames = ["reporter_id", "review_id"]
    )]
)
class ReviewReport(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    val review: Review
) : BaseCreatedEntity()