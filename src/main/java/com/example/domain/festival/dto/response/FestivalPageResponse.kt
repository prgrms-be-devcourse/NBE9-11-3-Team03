package com.example.domain.festival.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record FestivalPageResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        List<T> content
) {
    public static <T> FestivalPageResponse<T> from(Page<T> page) {
        return new FestivalPageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(), // 💡 다음 페이지 존재 여부 추가
                page.getContent()
        );
    }
}
