package com.example.domain.reviewreport.service

import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.reviewreport.dto.response.ReviewReportResponse
import com.example.domain.reviewreport.entity.ReviewReport
import com.example.domain.reviewreport.repository.ReviewReportRepository
import com.example.global.exception.BadRequestException
import com.example.global.exception.ConflictException
import com.example.global.exception.UnauthorizedException
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewReportService(
    private val reviewReportRepository: ReviewReportRepository,
    private val memberRepository: MemberRepository,
    private val reviewRepository: ReviewRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun reportReview(reviewId: Long, loginId: String): ReviewReportResponse {

        val reporter = memberRepository.findByLoginId(loginId)
            ?: throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")

        val review = reviewRepository.findByIdOrNull(reviewId)
            ?: throw EntityNotFoundException("존재하지 않는 리뷰입니다.")

        if (review.status == ReviewStatus.DELETED) {
            throw BadRequestException("삭제된 리뷰는 신고할 수 없습니다.")
        }

        if (review.member.id == reporter.id) {
            throw BadRequestException("본인 리뷰는 신고할 수 없습니다.")
        }

        if (reviewReportRepository.existsByReporterIdAndReviewId(reporter.id, reviewId)) {
            throw ConflictException("이미 신고한 리뷰입니다.")
        }

        val reviewReport = reviewReportRepository.save(ReviewReport(reporter, review))
        reviewRepository.increaseReportCount(reviewId)

        reviewRepository.findReportCountById(reviewId)
            ?.takeIf { it >= 5 }
            ?.let { log.warn("[Review] 신고 5회 임계치 - reviewId={}, reportCount={}", reviewId, it) }

        return ReviewReportResponse(reviewReport.id)
    }
}