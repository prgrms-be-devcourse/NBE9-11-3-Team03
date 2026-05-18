package com.example.domain.review.controller

import com.example.domain.review.service.ReviewLikeService
import com.example.domain.reviewlike.dto.response.ReviewLikeResponse
import com.example.global.response.ApiRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reviews/{reviewId}/like")
@Tag(name = "ReviewLike", description = "리뷰 좋아요 API")
class ReviewLikeController(private val reviewLikeService: ReviewLikeService) {

    @PostMapping
    @Operation(summary = "리뷰 좋아요", description = "특정 리뷰에 좋아요를 누릅니다.")
    fun likeReview(
        @PathVariable reviewId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewLikeResponse>> =
        reviewLikeService.likeReview(reviewId, authentication.name)
            .let { ResponseEntity.ok(ApiRes(200, "좋아요 상태가 변경되었습니다.", it)) }

    @DeleteMapping
    @Operation(summary = "리뷰 좋아요 취소", description = "특정 리뷰의 좋아요를 취소합니다.")
    fun cancelLikeReview(
        @PathVariable reviewId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewLikeResponse>> =
        reviewLikeService.cancelLikeReview(reviewId, authentication.name)
            .let { ResponseEntity.ok(ApiRes(200, "리뷰 좋아요가 취소되었습니다.", it)) }
}