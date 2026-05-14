package com.example.domain.review.dto;

import com.example.domain.review.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponseDto {

    private final Long reviewId;
    private final Long festivalId;
    private final Long memberId;
    private final String nickname;
    private final String content;
    private final String image;
    private final Integer rating;
    private final Integer likeCount;
    private final Integer reportCount;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ReviewResponseDto(Review review) {
        this.reviewId = review.getId();
        this.festivalId = review.getFestival().getId();
        this.memberId = review.getMember().getId();
        this.nickname = review.getMember().getNickname();
        this.content = review.getContent();
        this.image = review.getImage();
        this.rating = review.getRating();
        this.likeCount = review.getLikeCount();
        this.reportCount = review.getReportCount();
        this.status = review.getStatus().name();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }
}
