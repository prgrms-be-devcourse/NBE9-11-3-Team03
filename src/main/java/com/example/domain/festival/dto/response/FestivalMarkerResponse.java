package com.example.domain.festival.dto.response;

import com.example.domain.festival.entity.Festival;

public record FestivalMarkerResponse(
        Long id,
        String title,
        Double mapX,
        Double mapY
) {
    public static FestivalMarkerResponse from(Festival festival) {
        return new FestivalMarkerResponse(
                festival.getId(),
                festival.getTitle(),
                festival.getMapX(),
                festival.getMapY()
        );
    }
}
