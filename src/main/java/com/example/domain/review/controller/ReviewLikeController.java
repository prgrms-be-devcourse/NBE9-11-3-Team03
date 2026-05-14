package com.example.domain.review.controller;

import com.example.domain.review.service.ReviewLikeService;
import com.example.domain.reviewlike.dto.ReviewLikeResponseDto;
import com.example.global.response.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "ReviewLike", description = "리뷰 좋아요 API")
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

    @PostMapping("/reviews/{reviewId}/like")
    @Operation(summary = "리뷰 좋아요", description = "특정 리뷰에 좋아요를 누릅니다.")
    public ResponseEntity<ApiRes<ReviewLikeResponseDto>> likeReview(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        String loginId = authentication.getName();

        ReviewLikeResponseDto response = reviewLikeService.likeReview(reviewId, loginId);

        return ResponseEntity.ok(
                new ApiRes<>(200, "좋아요 상태가 변경되었습니다.", response)
        );
    }

    @DeleteMapping("/reviews/{reviewId}/like")
    @Operation(summary = "리뷰 좋아요 취소", description = "특정 리뷰의 좋아요를 취소합니다.")
    public ResponseEntity<ApiRes<ReviewLikeResponseDto>> cancelLikeReview(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        String loginId = authentication.getName();

        ReviewLikeResponseDto response = reviewLikeService.cancelLikeReview(reviewId, loginId);

        return ResponseEntity.ok(
                new ApiRes<>(200, "리뷰 좋아요가 취소되었습니다.", response)
        );
    }
}