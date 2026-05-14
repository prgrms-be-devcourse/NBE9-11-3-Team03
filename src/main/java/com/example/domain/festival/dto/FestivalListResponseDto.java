package com.example.domain.festival.dto;

import com.example.domain.festival.entity.Festival;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record FestivalListResponseDto(
        Long id,
        String title,
        String thumbnail,
        LocalDate startDate,
        LocalDate endDate,
        String address,
        String status,
        Integer viewCount,
        Integer bookMarkCount,
        Double averageRate,
        @JsonProperty("isBookmarked") boolean isBookmarked
) {
    public static FestivalListResponseDto from(Festival festival, boolean isBookmarked) {
        return new FestivalListResponseDto(
                festival.getId(),
                festival.getTitle(),
                festival.getThumbnailUrl(),
                festival.getStartDate().toLocalDate(),
                festival.getEndDate().toLocalDate(),
                festival.getAddress(),
                festival.getStatus().name(),
                festival.getViewCount(),
                festival.getBookMarkCount(),
                festival.getAverageRate(),
                isBookmarked
        );
    }
}
