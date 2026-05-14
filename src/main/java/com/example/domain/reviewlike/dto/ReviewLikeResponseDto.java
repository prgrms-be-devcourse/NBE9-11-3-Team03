package com.example.domain.reviewlike.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewLikeResponseDto {

    private Long reviewId;
    private Long memberId;
    private boolean isLiked;
    private Integer likeCount;
}
