package com.example.review.reviewReport;

import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.reviewreport.dto.ReviewReportResponse;
import com.example.domain.reviewreport.entity.ReviewReport;
import com.example.domain.reviewreport.repository.ReviewReportRepository;
import com.example.domain.reviewreport.service.ReviewReportService;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import com.example.global.exception.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
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
class ReviewReportServiceTest {

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReviewReportService reviewReportService;

    @Nested
    @DisplayName("리뷰 신고")
    class ReportReviewTest {

        @Test
        @DisplayName("신고 성공")
        void reportReview_success() {
            Long reviewId = 1L;
            String loginId = "reporter1";

            Member reporter = new Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0);
            setId(reporter, 10L);

            Member writer = new Member("writerId", "pw", "작성자", "writer@test.com", "작성자닉네임", 0);
            setId(writer, 20L);

            Review review = mock(Review.class);
            when(review.getStatus()).thenReturn(ReviewStatus.ACTIVE);
            when(review.getMember()).thenReturn(writer);   // 추가

            ReviewReport savedReport = mock(ReviewReport.class);
            when(savedReport.getId()).thenReturn(100L);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(reporter));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewReportRepository.existsByReporterIdAndReviewId(10L, reviewId)).thenReturn(false);
            when(reviewReportRepository.save(any(ReviewReport.class))).thenReturn(savedReport);

            ReviewReportResponse result = reviewReportService.reportReview(reviewId, loginId);

            assertThat(result.reportId()).isEqualTo(100L);
            verify(reviewReportRepository).save(any(ReviewReport.class));
            verify(reviewRepository).increaseReportCount(reviewId);
        }

        @Test
        @DisplayName("같은 유저 중복 신고")
        void reportReview_duplicate() {
            Long reviewId = 1L;
            String loginId = "reporter1";

            Member reporter = new Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0);
            setId(reporter, 10L);

            Member writer = new Member("writerId", "pw", "작성자", "writer@test.com", "작성자닉네임", 0);
            setId(writer, 20L); // reporter와 다른 사람이어야 함

            Review review = mock(Review.class);
            when(review.getStatus()).thenReturn(ReviewStatus.ACTIVE);
            when(review.getMember()).thenReturn(writer); // 추가

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(reporter));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(reviewReportRepository.existsByReporterIdAndReviewId(10L, reviewId)).thenReturn(true);

            assertThatThrownBy(() -> reviewReportService.reportReview(reviewId, loginId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("이미 신고한 리뷰입니다.");

            verify(reviewReportRepository, never()).save(any());
            verify(reviewRepository, never()).increaseReportCount(anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 신고")
        void reportReview_reviewNotFound() {
            Long reviewId = 999L;
            String loginId = "reporter1";

            Member reporter = new Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0);
            setId(reporter, 10L);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(reporter));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewReportService.reportReview(reviewId, loginId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("존재하지 않는 리뷰입니다.");
        }

        @Test
        @DisplayName("삭제된 리뷰 신고 시도")
        void reportReview_deletedReview() {
            Long reviewId = 1L;
            String loginId = "reporter1";

            Member reporter = new Member("loginId", "pw", "신고자", "reporter@test.com", "닉네임", 0);
            setId(reporter, 10L);

            Review review = mock(Review.class);
            when(review.getStatus()).thenReturn(ReviewStatus.DELETED);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(reporter));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewReportService.reportReview(reviewId, loginId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("삭제된 리뷰는 신고할 수 없습니다.");

            verify(reviewReportRepository, never()).save(any());
            verify(reviewRepository, never()).increaseReportCount(anyLong());
        }

        @Test
        @DisplayName("로그인 회원 정보 없음")
        void reportReview_memberNotFound() {
            Long reviewId = 1L;
            String loginId = "unknown";

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewReportService.reportReview(reviewId, loginId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("로그인한 회원 정보를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("자기 리뷰 신고 시도")
        void reportReview_selfReport() {
            Long reviewId = 1L;
            String loginId = "writer1";

            Member reporter = new Member("loginId", "pw", "작성자", "writer@test.com", "닉네임", 0);
            setId(reporter, 10L);

            Member writer = new Member("loginId2", "pw", "작성자", "writer2@test.com", "닉네임2", 0);
            setId(writer, 10L);

            Review review = mock(Review.class);
            when(review.getStatus()).thenReturn(ReviewStatus.ACTIVE);
            when(review.getMember()).thenReturn(writer);

            when(memberRepository.findByLoginId(loginId)).thenReturn(Optional.of(reporter));
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewReportService.reportReview(reviewId, loginId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("본인 리뷰는 신고할 수 없습니다.");

            verify(reviewReportRepository, never()).save(any());
            verify(reviewRepository, never()).increaseReportCount(anyLong());
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