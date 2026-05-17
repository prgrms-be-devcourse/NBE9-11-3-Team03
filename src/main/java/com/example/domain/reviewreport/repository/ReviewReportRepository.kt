package com.example.domain.reviewreport.repository;

import com.example.domain.reviewreport.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    // 같은 회원이 같은 리뷰를 이미 신고했는지 확인합니다.
    boolean existsByReporterIdAndReviewId(Long reporterId, Long reviewId);
}
