package com.example.domain.reviewreport.repository

import com.example.domain.reviewreport.entity.ReviewReport
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewReportRepository : JpaRepository<ReviewReport, Long> {
    fun existsByReporterIdAndReviewId(reporterId: Long, reviewId: Long): Boolean
}