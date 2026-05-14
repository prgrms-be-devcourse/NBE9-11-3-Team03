package com.example.domain.review.repository;

import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review,Long> {

    //특정 축제 리뷰 목록 조회(ACTIVE만 조회할 때 사용)
    Page<Review> findByFestivalIdAndStatus(Long festivalId, ReviewStatus status, Pageable pageable);

    //평균 평점
    @Query("select avg(r.rating) from Review r where r.festival.id = :festivalId and r.status = com.example.domain.review.entity.ReviewStatus.ACTIVE")
    Double calculateAverageRatingByFestivalId(@Param("festivalId") Long festivalId);

    Page<Review> findByFestivalId(Long festivalId, Pageable pageable);
    Page<Review> findAllByReportCountGreaterThanEqualAndStatus(int reportCount, ReviewStatus status, Pageable pageable);
    boolean existsByMemberIdAndFestivalIdAndStatus(Long memberId, Long festivalId, ReviewStatus status);

    long countByMemberIdAndStatus(Long memberId, ReviewStatus status);

    //사용자가 단 리뷰
    Page<Review> findByMemberIdAndStatus(Long memberId,ReviewStatus status,Pageable pageable);

    //리뷰 신고수 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reportCount = r.reportCount + 1 WHERE r.id = :reviewId")
    void increaseReportCount(@Param("reviewId") Long reviewId);

    // 리뷰 좋아요 수 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reviewId")
    void increaseLikeCount(@Param("reviewId") Long reviewId);

    // 리뷰 좋아요 수 감소 (0 이하로 떨어지지 않도록 조건 추가)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.likeCount = r.likeCount - 1 WHERE r.id = :reviewId AND r.likeCount > 0")
    void decreaseLikeCount(@Param("reviewId") Long reviewId);


    //리뷰를 블라인드로 바꾸는 함수
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review  r SET r.status = 'BLIND' WHERE r.id=:id AND r.status='ACTIVE'")
    int updateStatusToBlindActive(@Param("id") Long id);
    //리뷰가 ACITVE고 신고 횟수가 남아 있을때 0으로 바꾼느 메서드
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reportCount = 0 WHERE r.id=:id AND r.status='ACTIVE' AND r.reportCount>0")
    int resetReportCountIfActive(@Param("id") Long id);

}
