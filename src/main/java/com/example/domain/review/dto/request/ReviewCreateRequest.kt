package com.example.domain.review.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ReviewCreateRequest(

    @field:NotBlank
    @field:Schema(description = "리뷰 내용", example = "아이랑 가기 정말 좋았어요.")
    val content: String = "",

    @field:Schema(description = "리뷰 이미지 URL", example = "https://example.com/review-image.jpg")
    val image: String? = null,

    @field:Min(value = 1, message = "평점은 1점부터 5점까지 입력 가능합니다.")
    @field:Max(value = 5, message = "평점은 1점부터 5점까지 입력 가능합니다.")
    @field:Schema(description = "별점", example = "5")
    val rating: Int = 1
)
