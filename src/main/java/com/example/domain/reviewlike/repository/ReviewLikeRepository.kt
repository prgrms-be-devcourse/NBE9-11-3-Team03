package com.example.domain.reviewlike.repository;

import com.example.domain.reviewlike.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    boolean existsByMemberIdAndReviewId(Long memberId, Long reviewId);

    Optional<ReviewLike> findByMemberIdAndReviewId(Long memberId, Long reviewId);
}