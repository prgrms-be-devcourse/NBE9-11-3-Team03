package com.example.domain.review.service

import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.reviewlike.dto.response.ReviewLikeResponse
import com.example.domain.reviewlike.entity.ReviewLike
import com.example.domain.reviewlike.repository.ReviewLikeRepository
import com.example.global.exception.BadRequestException
import com.example.global.exception.ConflictException
import com.example.global.exception.CustomNotFoundException
import com.example.global.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReviewLikeService(
    private val reviewLikeRepository: ReviewLikeRepository,
    private val reviewRepository: ReviewRepository,
    private val memberRepository: MemberRepository
) {

    @Transactional
    fun likeReview(reviewId: Long, loginId: String): ReviewLikeResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.") }

        val review = reviewRepository.findById(reviewId)
            .orElseThrow { CustomNotFoundException("존재하지 않는 리뷰입니다.") }

        if (review.status == ReviewStatus.DELETED || review.status == ReviewStatus.BLIND) {
            throw BadRequestException("삭제되었거나 블라인드 처리된 리뷰에는 좋아요를 누를 수 없습니다.")
        }

        val alreadyLiked = reviewLikeRepository.existsByMemberIdAndReviewId(member.id, reviewId)
        if (alreadyLiked) {
            throw ConflictException("이미 좋아요를 누른 리뷰입니다.")
        }

        val reviewLike = ReviewLike(member, review)
        reviewLikeRepository.save(reviewLike)

        reviewRepository.increaseLikeCount(reviewId)

        return ReviewLikeResponse(
            reviewId = review.id,
            memberId = member.id,
            isLiked = true,
            likeCount = review.likeCount + 1
        )
    }

    @Transactional
    fun cancelLikeReview(reviewId: Long, loginId: String): ReviewLikeResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.") }

        val review = reviewRepository.findById(reviewId)
            .orElseThrow { CustomNotFoundException("존재하지 않는 리뷰입니다.") }

        val reviewLike = reviewLikeRepository.findByMemberIdAndReviewId(member.id, reviewId)
            .orElseThrow { BadRequestException("좋아요를 누르지 않은 리뷰입니다.") }

        reviewLikeRepository.delete(reviewLike)
        reviewLikeRepository.flush()

        reviewRepository.decreaseLikeCount(reviewId)

        return ReviewLikeResponse(
            reviewId = review.id,
            memberId = member.id,
            isLiked = false,
            likeCount = maxOf(0, review.likeCount - 1)
        )
    }
}
