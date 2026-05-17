package com.example.domain.reviewlike.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewLikeResponse {

    private Long reviewId;
    private Long memberId;
    private boolean isLiked;
    private Integer likeCount;
}
