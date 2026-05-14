package com.example.domain.member.dto.response;

import com.example.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;

public record MyReviewPageRes(
        List<MyReviewItemRes> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static MyReviewPageRes from (Page<Review> reviewPageewPage){
        return new MyReviewPageRes(
                reviewPageewPage.getContent().stream()
                        .map(MyReviewItemRes::from)
                        .toList(),
                reviewPageewPage.getNumber(),
                reviewPageewPage.getSize(),
                reviewPageewPage.getTotalElements(),
                reviewPageewPage.getTotalPages(),
                reviewPageewPage.hasNext()
        );
    }
}
