package com.example.domain.reviewreport.service;

import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.reviewreport.dto.ReviewReportResponse;
import com.example.domain.reviewreport.entity.ReviewReport;
import com.example.domain.reviewreport.repository.ReviewReportRepository;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import com.example.global.exception.UnauthorizedException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReviewReportResponse reportReview(Long reviewId, String loginId) {
        // 인증된 사용자의 loginId로 실제 신고 회원을 조회합니다.
        Member reporter = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        // 신고할 리뷰가 실제로 존재하는지 확인합니다.
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 리뷰입니다."));

        // 삭제된 리뷰는 사용자에게 보이지 않는 리뷰이므로 신고할 수 없도록 막습니다.
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BadRequestException("삭제된 리뷰는 신고할 수 없습니다.");
        }

        //본인 리뷰는 자기 자신이 신고할 수 없도록 막습니다
        if (review.getMember().getId().equals(reporter.getId())) {
            throw new BadRequestException("본인 리뷰는 신고할 수 없습니다.");
        }

        // 한 회원이 같은 리뷰를 여러 번 신고하지 못하도록 먼저 중복 여부를 확인합니다.
        if (reviewReportRepository.existsByReporterIdAndReviewId(reporter.getId(), reviewId)) {
            throw new ConflictException("이미 신고한 리뷰입니다.");
        }

        // 신고 정보를 저장하고, 리뷰의 누적 신고 수도 함께 증가시킵니다.
        ReviewReport reviewReport = reviewReportRepository.save(new ReviewReport(reporter, review));

        reviewRepository.increaseReportCount(reviewId);

        return new ReviewReportResponse(reviewReport.getId());
    }
}
