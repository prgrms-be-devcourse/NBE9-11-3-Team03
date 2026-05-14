package com.example.domain.review.dto;


import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewListResponseDto {

    private Long reviewId;
    private Long memberId;
    private Long festivalId;
    private String nickname;
    private String content;
    private Integer rating;
    private String image;
    private Integer likeCount;
    private Integer reportCount;
    private LocalDateTime createdAt;
    private boolean liked;

    public static ReviewListResponseDto from(Review review, boolean liked) {
        Member member = review.getMember();
        String displayName = (member.getStatus() == MemberStatus.WITHDRAWN)
                ? "탈퇴된 회원입니다."
                : member.getNickname();

        return ReviewListResponseDto.builder()
                .reviewId(review.getId())
                .memberId(review.getMember().getId())
                .festivalId(review.getFestival().getId())
                .nickname(displayName)
                .content(review.getContent())
                .image(review.getImage())
                .rating(review.getRating())
                .likeCount(review.getLikeCount())
                .reportCount(review.getReportCount())
                .createdAt(review.getCreatedAt())
                .liked(liked)
                .build();


    }
}
