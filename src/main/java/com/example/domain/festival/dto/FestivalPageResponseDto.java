package com.example.domain.festival.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record FestivalPageResponseDto<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        List<T> content
) {
    public static <T> FestivalPageResponseDto<T> from(Page<T> page) {
        return new FestivalPageResponseDto<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(), // 💡 다음 페이지 존재 여부 추가
                page.getContent()
        );
    }
}
