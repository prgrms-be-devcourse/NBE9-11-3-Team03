package com.example.domain.review.service;

import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.reviewlike.dto.ReviewLikeResponseDto;
import com.example.domain.reviewlike.entity.ReviewLike;
import com.example.domain.reviewlike.repository.ReviewLikeRepository;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import com.example.global.exception.CustomNotFoundException;
import com.example.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReviewLikeResponseDto likeReview(Long reviewId, String loginId) {

        // 1. 로그인 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        // 2. 리뷰 존재 여부 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 리뷰입니다."));

        // 3. 삭제/블라인드 리뷰 좋아요 불가
        if (review.getStatus() == ReviewStatus.DELETED || review.getStatus() == ReviewStatus.BLIND) {
            throw new BadRequestException("삭제되었거나 블라인드 처리된 리뷰에는 좋아요를 누를 수 없습니다.");
        }

        // 4. 중복 좋아요 검사
        boolean alreadyLiked = reviewLikeRepository.existsByMemberIdAndReviewId(member.getId(), reviewId);
        if (alreadyLiked) {
            throw new ConflictException("이미 좋아요를 누른 리뷰입니다.");
        }

        // 5. 좋아요 저장
        ReviewLike reviewLike = new ReviewLike(member, review);
        reviewLikeRepository.save(reviewLike);

        // 6. 리뷰 좋아요 수 증가
        reviewRepository.increaseLikeCount(reviewId);

        // 7. 응답 반환
        return new ReviewLikeResponseDto(
                review.getId(),
                member.getId(),
                true,
                review.getLikeCount()+1
        );
    }

    @Transactional
    public ReviewLikeResponseDto cancelLikeReview(Long reviewId, String loginId) {

        // 1. 로그인 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        // 2. 리뷰 존재 여부 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 리뷰입니다."));

        // 3. 좋아요 이력 확인
        ReviewLike reviewLike = reviewLikeRepository.findByMemberIdAndReviewId(member.getId(), reviewId)
                .orElseThrow(() -> new BadRequestException("좋아요를 누르지 않은 리뷰입니다."));

        // 4. 좋아요 삭제
        reviewLikeRepository.delete(reviewLike);
        reviewLikeRepository.flush();

        // 5. 리뷰 좋아요 수 감소
        reviewRepository.decreaseLikeCount(reviewId);

        // 6. 응답 반환
        return new ReviewLikeResponseDto(
                review.getId(),
                member.getId(),
                false,
                Math.max(0, review.getLikeCount() - 1)
        );
    }
}
