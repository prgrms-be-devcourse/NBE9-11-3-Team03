package com.example.review.reviewLike;

import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.review.service.ReviewLikeService;
import com.example.domain.reviewlike.dto.ReviewLikeResponseDto;
import com.example.domain.reviewlike.entity.ReviewLike;
import com.example.domain.reviewlike.repository.ReviewLikeRepository;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import com.example.global.exception.CustomNotFoundException;
import com.example.global.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewLikeServiceTest {

    @Mock
    private ReviewLikeRepository reviewLikeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReviewLikeService reviewLikeService;

    @Nested
    @DisplayName("리뷰 좋아요")
    class LikeReviewTest {

        @Test
        @DisplayName("좋아요 성공")
        void likeReview_success() {
            // given
            Long reviewId = 10L;
            String loginId = "user1";

            Member member = new Member("loginId", "pw", "홍길동", "user@test.com", "닉네임", 0);
            setId(member, 1L);

            Review review = mock(Review.class);
            when(review.getId()).thenReturn(reviewId);
            when(review.getStatus()).thenReturn(com.example.domain.review.entity.ReviewStatus.ACTIVE);
            when(review.getLikeCount()).thenReturn(0);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewLikeRepository.existsByMemberIdAndReviewId(1L, reviewId)).thenReturn(false);

            // when
            ReviewLikeResponseDto result = reviewLikeService.likeReview(reviewId, loginId);

            // then
            assertThat(result.getReviewId()).isEqualTo(reviewId);
            assertThat(result.getMemberId()).isEqualTo(1L);
            assertThat(result.isLiked()).isTrue();
            assertThat(result.getLikeCount()).isEqualTo(1);

            verify(reviewLikeRepository).save(any(ReviewLike.class));
            verify(reviewRepository).increaseLikeCount(reviewId);
        }

        @Test
        @DisplayName("같은 유저 중복 좋아요")
        void likeReview_duplicate() {
            // given
            Long reviewId = 10L;
            String loginId = "user1";

            Member member = new Member("loginId", "pw", "홍길동", "user@test.com", "닉네임", 0);
            setId(member, 1L);

            Review review = mock(Review.class);
            when(review.getStatus()).thenReturn(com.example.domain.review.entity.ReviewStatus.ACTIVE);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewLikeRepository.existsByMemberIdAndReviewId(1L, reviewId)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewLikeService.likeReview(reviewId, loginId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("이미 좋아요를 누른 리뷰입니다.");

            verify(reviewLikeRepository, never()).save(any());
            verify(reviewRepository, never()).increaseLikeCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 좋아요 시도")
        void likeReview_reviewNotFound() {
            // given
            Long reviewId = 999L;
            String loginId = "user1";

            Member member = new Member("loginId", "pw", "홍길동", "user@test.com", "닉네임", 0);
            setId(member, 1L);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewLikeService.likeReview(reviewId, loginId))
                    .isInstanceOf(CustomNotFoundException.class)
                    .hasMessage("존재하지 않는 리뷰입니다.");
        }

        @Test
        @DisplayName("로그인 회원 정보 없음")
        void likeReview_memberNotFound() {
            // given
            Long reviewId = 10L;
            String loginId = "unknown";

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewLikeService.likeReview(reviewId, loginId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("로그인한 회원 정보를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("리뷰 좋아요 취소")
    class CancelLikeReviewTest {

        @Test
        @DisplayName("좋아요 취소 성공")
        void cancelLikeReview_success() {
            // given
            Long reviewId = 10L;
            String loginId = "user1";

            Member member = new Member("loginId", "pw", "홍길동", "user@test.com", "닉네임", 0);
            setId(member, 1L);

            Review review = mock(Review.class);
            when(review.getId()).thenReturn(reviewId);
            when(review.getLikeCount()).thenReturn(1);

            ReviewLike reviewLike = mock(ReviewLike.class);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewLikeRepository.findByMemberIdAndReviewId(1L, reviewId)).thenReturn(Optional.of(reviewLike));

            // when
            ReviewLikeResponseDto result = reviewLikeService.cancelLikeReview(reviewId, loginId);

            // then
            assertThat(result.getReviewId()).isEqualTo(reviewId);
            assertThat(result.getMemberId()).isEqualTo(1L);
            assertThat(result.isLiked()).isFalse();
            assertThat(result.getLikeCount()).isEqualTo(0);

            verify(reviewLikeRepository).delete(reviewLike);
            verify(reviewLikeRepository).flush();
            verify(reviewRepository).decreaseLikeCount(reviewId);
        }

        @Test
        @DisplayName("좋아요 누르지 않은 상태에서 취소")
        void cancelLikeReview_withoutLike() {
            // given
            Long reviewId = 10L;
            String loginId = "user1";

            Member member = new Member("loginId", "pw", "홍길동", "user@test.com", "닉네임", 0);
            setId(member, 1L);

            Review review = mock(Review.class);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(member));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewLikeRepository.findByMemberIdAndReviewId(1L, reviewId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewLikeService.cancelLikeReview(reviewId, loginId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("좋아요를 누르지 않은 리뷰입니다.");

            verify(reviewLikeRepository, never()).delete(any());
            verify(reviewRepository, never()).decreaseLikeCount(anyLong());
        }
    }

    private void setId(Object target, Long id) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(target, id);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new IllegalArgumentException("id 필드를 찾을 수 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}