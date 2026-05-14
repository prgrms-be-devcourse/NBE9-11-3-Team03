package com.example.domain.reviewlike.entity;

import com.example.domain.member.entity.Member;
import com.example.domain.review.entity.Review;
import com.example.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_like_member_review",
                        columnNames = {"member_id", "review_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewLike extends BaseCreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    //TODOS 생성자 구현

    public ReviewLike(Member member, Review review) {
        this.member = member;
        this.review = review;
    }
}
