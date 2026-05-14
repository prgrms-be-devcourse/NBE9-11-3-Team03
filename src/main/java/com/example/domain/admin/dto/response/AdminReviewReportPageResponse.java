package com.example.domain.admin.dto.response;

import com.example.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminReviewReportPageResponse(
        List<AdminReviewReportResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminReviewReportPageResponse from(Page<Review> reviewPage){
        return new AdminReviewReportPageResponse(
                reviewPage.getContent().stream()
                        .map(AdminReviewReportResponse::from)
                        .toList(),
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()

        );
    }
}
