package com.example.domain.member.dto.response;

import com.example.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;

public record MyReviewPageResponse(
        List<MyReviewItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static MyReviewPageResponse from (Page<Review> reviewPageewPage){
        return new MyReviewPageResponse(
                reviewPageewPage.getContent().stream()
                        .map(MyReviewItemResponse::from)
                        .toList(),
                reviewPageewPage.getNumber(),
                reviewPageewPage.getSize(),
                reviewPageewPage.getTotalElements(),
                reviewPageewPage.getTotalPages(),
                reviewPageewPage.hasNext()
        );
    }
}
