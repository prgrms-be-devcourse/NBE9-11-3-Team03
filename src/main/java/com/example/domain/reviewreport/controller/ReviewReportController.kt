package com.example.domain.reviewreport.controller

import com.example.domain.reviewreport.dto.response.ReviewReportResponse
import com.example.domain.reviewreport.service.ReviewReportService
import com.example.global.response.ApiRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "Review Report", description = "리뷰 신고 API")
class ReviewReportController(private val reviewReportService: ReviewReportService) {
    @PostMapping("/reviews/{reviewId}/reports")
    @Operation(
        summary = "리뷰 신고",
        description = "부적절한 리뷰를 신고합니다. 같은 리뷰를 중복 신고할 수 없습니다. 신고 누적 시 관리자가 리뷰를 블라인드 처리할 수 있습니다."
    )
    //리뷰를 신고하는 컨트롤러
    fun reportReview(
        @PathVariable reviewId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiRes<ReviewReportResponse>> {
        val response = reviewReportService.reportReview(reviewId, authentication.name)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiRes(201, "리뷰 신고가 성공적으로 접수되었습니다.", response))
    }
}