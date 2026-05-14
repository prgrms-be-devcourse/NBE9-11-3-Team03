package com.example.domain.admin.dto;

import com.example.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminReviewReportPageRes(
        List<AdminReviewReportRes> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminReviewReportPageRes from(Page<Review> reviewPage){
        return new AdminReviewReportPageRes(
                reviewPage.getContent().stream()
                        .map(AdminReviewReportRes::from)
                        .toList(),
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()

        );
    }
}
