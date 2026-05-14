package com.example.domain.festival.dto.response;

import com.example.domain.festival.entity.Festival;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record FestivalListResponse(
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
    public static FestivalListResponse from(Festival festival, boolean isBookmarked) {
        return new FestivalListResponse(
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
