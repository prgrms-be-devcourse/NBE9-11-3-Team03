package com.example.review.reviewReport

import com.example.domain.member.entity.Member
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.reviewreport.entity.ReviewReport
import com.example.domain.reviewreport.repository.ReviewReportRepository
import com.example.domain.reviewreport.service.ReviewReportService
import com.example.global.exception.BadRequestException
import com.example.global.exception.ConflictException
import com.example.global.exception.UnauthorizedException
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.BDDMockito.verify
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ReviewReportServiceTest {

    @Mock
    private lateinit var reviewReportRepository: ReviewReportRepository

    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @InjectMocks
    private lateinit var reviewReportService: ReviewReportService

    @Nested
    @DisplayName("리뷰 신고")
    inner class ReportReviewTest {

        @Test
        @DisplayName("신고 성공")
        fun reportReview_success() {
            // given
            val reviewId = 1L
            val loginId = "reporter1"

            val reporter = Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0).apply {
                setId(this, 10L)
            }

            val writer = Member("writerId", "pw", "작성자", "writer@test.com", "작성자닉네임", 0).apply {
                setId(this, 20L)
            }

            val review = mock(Review::class.java)
            given(review.status).willReturn(ReviewStatus.ACTIVE)
            given(review.member).willReturn(writer)

            val savedReport = mock(ReviewReport::class.java)
            given(savedReport.id).willReturn(100L)

            // memberRepository는 엔티티 직접 반환 처리
            given(memberRepository.findByLoginId(loginId)).willReturn(reporter)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))
            given(reviewReportRepository.existsByReporterIdAndReviewId(10L, reviewId)).willReturn(false)
            given(reviewReportRepository.save(any(ReviewReport::class.java))).willReturn(savedReport)

            // when
            val result = reviewReportService.reportReview(reviewId, loginId)

            // then
            assertThat(result.reportId).isEqualTo(100L)
            verify(reviewReportRepository).save(any(ReviewReport::class.java))
            verify(reviewRepository).increaseReportCount(reviewId)
        }

        @Test
        @DisplayName("같은 유저 중복 신고")
        fun reportReview_duplicate() {
            // given
            val reviewId = 1L
            val loginId = "reporter1"

            val reporter = Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0).apply {
                setId(this, 10L)
            }

            val writer = Member("writerId", "pw", "작성자", "writer@test.com", "작성자닉네임", 0).apply {
                setId(this, 20L) // reporter와 다른 사람이어야 함
            }

            val review = mock(Review::class.java)
            given(review.status).willReturn(ReviewStatus.ACTIVE)
            given(review.member).willReturn(writer)

            given(memberRepository.findByLoginId(loginId)).willReturn(reporter)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))
            given(reviewReportRepository.existsByReporterIdAndReviewId(10L, reviewId)).willReturn(true)

            // when & then
            assertThatThrownBy { reviewReportService.reportReview(reviewId, loginId) }
                .isInstanceOf(ConflictException::class.java)
                .hasMessage("이미 신고한 리뷰입니다.")

            verify(reviewReportRepository, never()).save(any())
            verify(reviewRepository, never()).increaseReportCount(anyLong())
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 신고")
        fun reportReview_reviewNotFound() {
            // given
            val reviewId = 999L
            val loginId = "reporter1"

            val reporter = Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0).apply {
                setId(this, 10L)
            }

            given(memberRepository.findByLoginId(loginId)).willReturn(reporter)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.empty())

            // when & then
            assertThatThrownBy { reviewReportService.reportReview(reviewId, loginId) }
                .isInstanceOf(EntityNotFoundException::class.java)
                .hasMessage("존재하지 않는 리뷰입니다.")
        }

        @Test
        @DisplayName("삭제된 리뷰 신고 시도")
        fun reportReview_deletedReview() {
            // given
            val reviewId = 1L
            val loginId = "reporter1"

            val reporter = Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0).apply {
                setId(this, 10L)
            }

            val review = mock(Review::class.java)
            given(review.status).willReturn(ReviewStatus.DELETED)

            given(memberRepository.findByLoginId(loginId)).willReturn(reporter)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))

            // when & then
            assertThatThrownBy { reviewReportService.reportReview(reviewId, loginId) }
                .isInstanceOf(BadRequestException::class.java)
                .hasMessage("삭제된 리뷰는 신고할 수 없습니다.")

            verify(reviewReportRepository, never()).save(any())
            verify(reviewRepository, never()).increaseReportCount(anyLong())
        }

        @Test
        @DisplayName("로그인 회원 정보 없음")
        fun reportReview_memberNotFound() {
            // given
            val reviewId = 1L
            val loginId = "unknown"

            // nullable이므로 null 반환
            given(memberRepository.findByLoginId(loginId)).willReturn(null)

            // when & then
            assertThatThrownBy { reviewReportService.reportReview(reviewId, loginId) }
                .isInstanceOf(UnauthorizedException::class.java)
                .hasMessage("로그인한 회원 정보를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("자기 리뷰 신고 시도")
        fun reportReview_selfReport() {
            // given
            val reviewId = 1L
            val loginId = "writer1"

            // reporter와 writer의 ID가 같도록 설정 (본인 리뷰 신고 테스트)
            val reporter = Member("loginId", "pw", "작성자", "writer@test.com", "닉네임", 0).apply {
                setId(this, 10L)
            }

            val writer = Member("loginId2", "pw", "작성자", "writer2@test.com", "닉네임2", 0).apply {
                setId(this, 10L)
            }

            val review = mock(Review::class.java)
            given(review.status).willReturn(ReviewStatus.ACTIVE)
            given(review.member).willReturn(writer)

            given(memberRepository.findByLoginId(loginId)).willReturn(reporter)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))

            // when & then
            assertThatThrownBy { reviewReportService.reportReview(reviewId, loginId) }
                .isInstanceOf(BadRequestException::class.java)
                .hasMessage("본인 리뷰는 신고할 수 없습니다.")

            verify(reviewReportRepository, never()).save(any())
            verify(reviewRepository, never()).increaseReportCount(anyLong())
        }
    }

    private fun setId(target: Any, id: Long) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField("id")
                field.isAccessible = true
                field.set(target, id)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        throw IllegalArgumentException("id 필드를 찾을 수 없습니다.")
    }
}