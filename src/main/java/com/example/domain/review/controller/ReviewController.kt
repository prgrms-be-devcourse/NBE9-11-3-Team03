package com.example.domain.review.controller

import com.example.domain.review.dto.request.ReviewCreateRequest
import com.example.domain.review.dto.request.ReviewUpdateRequest
import com.example.domain.review.dto.response.ReviewDeleteResponse
import com.example.domain.review.dto.response.ReviewPageResponse
import com.example.domain.review.dto.response.ReviewResponse
import com.example.domain.review.dto.response.ReviewUpdateResponse
import com.example.domain.review.service.ReviewService
import com.example.global.response.ApiRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
@Tag(name = "Review", description = "리뷰 API")
class ReviewController(
    private val reviewService: ReviewService
) {

    @PostMapping(
        value = ["/festivals/{festivalId}/reviews"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @Operation(summary = "축제 리뷰 작성", description = "특정 축제에 리뷰와 사진을 함께 작성합니다.")
    fun createReview(
        @PathVariable festivalId: Long,
        @Valid @RequestPart("requestDto") requestDto: ReviewCreateRequest,
        @RequestPart(value = "image", required = false) image: MultipartFile?,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewResponse>> {
        val loginId = authentication.name
        val response = reviewService.createReview(festivalId, loginId, requestDto, image)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiRes(201, "리뷰 작성이 완료 되었습니다.", response))
    }

    @GetMapping("/festivals/{festivalId}/reviews")
    @Operation(summary = "축제 리뷰 목록 조회", description = "특정 축제의 리뷰 목록을 페이징하여 조회합니다.")
    fun getReviewList(
        @PathVariable festivalId: Long,
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10") size: Int,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewPageResponse>> {
        val loginId = authentication.name
        val response = reviewService.getReviewList(festivalId, loginId, page, size)

        return ResponseEntity.ok(
            ApiRes(200, "축제 리뷰 목록 조회 성공", response)
        )
    }

    @PatchMapping(
        value = ["/reviews/{reviewId}"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @Operation(summary = "축제 리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다.")
    fun updateReview(
        @PathVariable reviewId: Long,
        @Valid @RequestPart("requestDto") requestDto: ReviewUpdateRequest,
        @RequestPart(value = "image", required = false) image: MultipartFile?,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewUpdateResponse>> {
        val loginId = authentication.name
        val response = reviewService.updateReview(reviewId, loginId, requestDto, image)

        return ResponseEntity.ok(
            ApiRes(200, "리뷰 수정 완료", response)
        )
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "축제 리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다.")
    fun deleteReview(
        @PathVariable reviewId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewDeleteResponse>> {
        val loginId = authentication.name
        val response = reviewService.deleteReview(reviewId, loginId)

        return ResponseEntity.ok(
            ApiRes(200, "리뷰 삭제가 완료되었습니다.", response)
        )
    }
}
