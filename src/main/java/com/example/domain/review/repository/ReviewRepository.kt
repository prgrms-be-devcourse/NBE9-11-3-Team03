package com.example.domain.review.repository

import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewRepository : JpaRepository<Review, Long> {

    // 특정 축제 리뷰 목록 조회(ACTIVE만 조회할 때 사용)
    fun findByFestivalIdAndStatus(festivalId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    // 평균 평점
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.festival.id = :festivalId AND r.status = com.example.domain.review.entity.ReviewStatus.ACTIVE")
    fun calculateAverageRatingByFestivalId(@Param("festivalId") festivalId: Long): Double?

    fun findByFestivalId(festivalId: Long, pageable: Pageable): Page<Review>

    fun findAllByReportCountGreaterThanEqualAndStatus(
        reportCount: Int,
        status: ReviewStatus,
        pageable: Pageable
    ): Page<Review>

    fun existsByMemberIdAndFestivalIdAndStatus(memberId: Long, festivalId: Long, status: ReviewStatus): Boolean

    fun countByMemberIdAndStatus(memberId: Long, status: ReviewStatus): Long

    // 사용자가 단 리뷰
    fun findByMemberIdAndStatus(memberId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    // 리뷰 신고수 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reportCount = r.reportCount + 1 WHERE r.id = :reviewId")
    fun increaseReportCount(@Param("reviewId") reviewId: Long)

    // 현재 리뷰 신고 수 조회
    @Query("SELECT r.reportCount FROM Review r WHERE r.id = :reviewId")
    fun findReportCountById(@Param("reviewId") reviewId: Long): Int?

    // 리뷰 좋아요 수 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reviewId")
    fun increaseLikeCount(@Param("reviewId") reviewId: Long)

    // 리뷰 좋아요 수 감소 (0 이하로 떨어지지 않도록 조건 추가)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.likeCount = r.likeCount - 1 WHERE r.id = :reviewId AND r.likeCount > 0")
    fun decreaseLikeCount(@Param("reviewId") reviewId: Long)

    // 리뷰를 블라인드로 바꾸는 함수
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.status = 'BLIND' WHERE r.id = :id AND r.status = 'ACTIVE'")
    fun updateStatusToBlindActive(@Param("id") id: Long): Int

    // 리뷰가 ACTIVE고 신고 횟수가 남아 있을때 0으로 바꾸는 메서드
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reportCount = 0 WHERE r.id = :id AND r.status = 'ACTIVE' AND r.reportCount > 0")
    fun resetReportCountIfActive(@Param("id") id: Long): Int
}