package com.example.review.reviewLike

import com.example.domain.member.entity.Member
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.review.service.ReviewLikeService
import com.example.domain.reviewlike.entity.ReviewLike
import com.example.domain.reviewlike.repository.ReviewLikeRepository
import com.example.global.exception.BadRequestException
import com.example.global.exception.ConflictException
import com.example.global.exception.CustomNotFoundException
import com.example.global.exception.UnauthorizedException
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
class ReviewLikeServiceTest {

    @Mock
    private lateinit var reviewLikeRepository: ReviewLikeRepository

    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @InjectMocks
    private lateinit var reviewLikeService: ReviewLikeService

    @Nested
    @DisplayName("리뷰 좋아요")
    inner class LikeReviewTest { // 코틀린에서는 Nested 클래스에 inner를 붙여야 외부 프로퍼티에 접근 가능합니다.

        @Test
        @DisplayName("좋아요 성공")
        fun likeReview_success() {
            // given
            val reviewId = 10L
            val loginId = "user1"

            // apply를 활용해 생성과 동시에 id 세팅
            val member = Member.create("loginId", "pw", "홍길동", "user@test.com", "닉네임").apply {
                setId(this, 1L)
            }

            val review = mock(Review::class.java)
            given(review.id).willReturn(reviewId)
            given(review.status).willReturn(ReviewStatus.ACTIVE)
            given(review.likeCount).willReturn(0)

            // 코틀린 서비스 단에서 nullable로 받으므로 Optional 대신 직접 리턴
            given(memberRepository.findByLoginId(loginId)).willReturn(member)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))
            given(reviewLikeRepository.existsByMemberIdAndReviewId(1L, reviewId)).willReturn(false)

            // when
            val result = reviewLikeService.likeReview(reviewId, loginId)

            // then
            assertThat(result.reviewId).isEqualTo(reviewId)
            assertThat(result.memberId).isEqualTo(1L)
            assertThat(result.isLiked).isTrue()
            assertThat(result.likeCount).isEqualTo(1)

            verify(reviewLikeRepository).save(any(ReviewLike::class.java))
            verify(reviewRepository).increaseLikeCount(reviewId)
        }

        @Test
        @DisplayName("같은 유저 중복 좋아요")
        fun likeReview_duplicate() {
            // given
            val reviewId = 10L
            val loginId = "user1"

            val member = Member.create("loginId", "pw", "홍길동", "user@test.com", "닉네임").apply {
                setId(this, 1L)
            }

            val review = mock(Review::class.java)
            given(review.status).willReturn(ReviewStatus.ACTIVE)

            given(memberRepository.findByLoginId(loginId)).willReturn(member)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))
            given(reviewLikeRepository.existsByMemberIdAndReviewId(1L, reviewId)).willReturn(true)

            // when & then
            assertThatThrownBy { reviewLikeService.likeReview(reviewId, loginId) }
                .isInstanceOf(ConflictException::class.java)
                .hasMessage("이미 좋아요를 누른 리뷰입니다.")

            verify(reviewLikeRepository, never()).save(any())
            verify(reviewRepository, never()).increaseLikeCount(anyLong())
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 좋아요 시도")
        fun likeReview_reviewNotFound() {
            // given
            val reviewId = 999L
            val loginId = "user1"

            val member = Member.create("loginId", "pw", "홍길동", "user@test.com", "닉네임").apply {
                setId(this, 1L)
            }

            given(memberRepository.findByLoginId(loginId)).willReturn(member)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.empty())

            // when & then
            assertThatThrownBy { reviewLikeService.likeReview(reviewId, loginId) }
                .isInstanceOf(CustomNotFoundException::class.java)
                .hasMessage("존재하지 않는 리뷰입니다.")
        }

        @Test
        @DisplayName("로그인 회원 정보 없음")
        fun likeReview_memberNotFound() {
            // given
            val reviewId = 10L
            val loginId = "unknown"

            // nullable 반환이므로 null 세팅
            given(memberRepository.findByLoginId(loginId)).willReturn(null)

            // when & then
            assertThatThrownBy { reviewLikeService.likeReview(reviewId, loginId) }
                .isInstanceOf(UnauthorizedException::class.java)
                .hasMessage("로그인한 회원 정보를 찾을 수 없습니다.")
        }
    }

    @Nested
    @DisplayName("리뷰 좋아요 취소")
    inner class CancelLikeReviewTest {

        @Test
        @DisplayName("좋아요 취소 성공")
        fun cancelLikeReview_success() {
            // given
            val reviewId = 10L
            val loginId = "user1"

            val member = Member.create("loginId", "pw", "홍길동", "user@test.com", "닉네임").apply {
                setId(this, 1L)
            }

            val review = mock(Review::class.java)
            given(review.id).willReturn(reviewId)
            given(review.likeCount).willReturn(1)

            val reviewLike = mock(ReviewLike::class.java)

            given(memberRepository.findByLoginId(loginId)).willReturn(member)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))
            given(reviewLikeRepository.findByMemberIdAndReviewId(1L, reviewId)).willReturn(reviewLike)

            // when
            val result = reviewLikeService.cancelLikeReview(reviewId, loginId)

            // then
            assertThat(result.reviewId).isEqualTo(reviewId)
            assertThat(result.memberId).isEqualTo(1L)
            assertThat(result.isLiked).isFalse()
            assertThat(result.likeCount).isEqualTo(0)

            verify(reviewLikeRepository).delete(reviewLike)
            verify(reviewLikeRepository).flush()
            verify(reviewRepository).decreaseLikeCount(reviewId)
        }

        @Test
        @DisplayName("좋아요 누르지 않은 상태에서 취소")
        fun cancelLikeReview_withoutLike() {
            // given
            val reviewId = 10L
            val loginId = "user1"

            val member = Member.create("loginId", "pw", "홍길동", "user@test.com", "닉네임",).apply {
                setId(this, 1L)
            }

            val review = mock(Review::class.java)

            given(memberRepository.findByLoginId(loginId)).willReturn(member)
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review))
            given(reviewLikeRepository.findByMemberIdAndReviewId(1L, reviewId)).willReturn(null)

            // when & then
            assertThatThrownBy { reviewLikeService.cancelLikeReview(reviewId, loginId) }
                .isInstanceOf(BadRequestException::class.java)
                .hasMessage("좋아요를 누르지 않은 리뷰입니다.")

            verify(reviewLikeRepository, never()).delete(any())
            verify(reviewRepository, never()).decreaseLikeCount(anyLong())
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