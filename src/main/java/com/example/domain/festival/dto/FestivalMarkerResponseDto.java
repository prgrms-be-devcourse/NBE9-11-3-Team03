package com.example.domain.festival.dto;

import com.example.domain.festival.entity.Festival;

public record FestivalMarkerResponseDto(
        Long id,
        String title,
        Double mapX,
        Double mapY
) {
    public static FestivalMarkerResponseDto from(Festival festival) {
        return new FestivalMarkerResponseDto(
                festival.getId(),
                festival.getTitle(),
                festival.getMapX(),
                festival.getMapY()
        );
    }
}
