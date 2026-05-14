package com.example.domain.festival.dto;

import com.example.domain.festival.entity.FestivalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record FestivalSearchRequestDto(
        @Schema(description = "지역 코드 (예: 11)", nullable = true)
        String regionCode,

        @Schema(description = "진행 상태 (예: ONGOING)", nullable = true)
        FestivalStatus status,

        @Schema(description = "축제 시작 월 (1~12)", nullable = true)
        Integer month,

        @Schema(description = "검색어", nullable = true)
        String keyword,

        @Schema(description = "내 위치 경도", nullable = true)
        Double mapX,

        @Schema(description = "내 위치 위도", nullable = true)
        Double mapY,

        @Schema(description = "검색 반경(km)", nullable = true)
        Double radiusKm
) {
    public FestivalSearchRequestDto applyMapDefaults() {
        return new FestivalSearchRequestDto(
                this.regionCode,
                this.status,
                this.month,
                this.keyword,
                this.mapX != null ? this.mapX : 126.9780, // 서울시청 X
                this.mapY != null ? this.mapY : 37.5665,  // 서울시청 Y
                this.radiusKm != null ? this.radiusKm : 10.0 // 기본 반경
        );
    }
}
