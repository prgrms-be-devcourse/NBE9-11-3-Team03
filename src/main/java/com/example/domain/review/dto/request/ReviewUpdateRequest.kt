package com.example.domain.review.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ReviewUpdateRequest(

    @field:NotBlank(message = "리뷰 내용은 필수입니다.")
    val content: String? = null,

    val isDeleteImage: Boolean = false,

    @field:NotNull(message = "평점은 필수입니다.")
    @field:Min(value = 1, message = "평점은 1점부터 5점까지 입력 가능합니다.")
    @field:Max(value = 5, message = "평점은 1점부터 5점까지 입력 가능합니다.")
    val rating: Int? = null
)