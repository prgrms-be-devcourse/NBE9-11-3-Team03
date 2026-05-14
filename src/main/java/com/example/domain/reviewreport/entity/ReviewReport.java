package com.example.domain.reviewreport.entity;

import com.example.domain.member.entity.Member;
import com.example.domain.review.entity.Review;
import com.example.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_report_reporter_review",
                        columnNames = {"reporter_id", "review_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport extends BaseCreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // 신고한 회원과 신고 대상 리뷰를 받아 review_report 데이터를 생성합니다.
    public ReviewReport(Member reporter, Review review) {
        this.reporter = reporter;
        this.review = review;
    }

    //TODOS 생성자 구현
}
