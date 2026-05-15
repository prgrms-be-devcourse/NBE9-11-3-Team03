package com.example.domain.festival.dto.request

import com.example.domain.festival.entity.FestivalStatus
import io.swagger.v3.oas.annotations.media.Schema

data class FestivalSearchRequest(
    @Schema(description = "지역 코드 (예: 11)", nullable = true)
     val regionCode: String?= null,

    @Schema(description = "진행 상태 (예: ONGOING)", nullable = true)
     val status: FestivalStatus?= null,

    @Schema(description = "축제 시작 월 (1~12)", nullable = true)
     val month: Int?= null,

    @Schema(description = "검색어", nullable = true)
     val keyword: String?= null,

    @Schema(description = "내 위치 경도", nullable = true)
     val mapX: Double?= null,

    @Schema(description = "내 위치 위도", nullable = true)
     val mapY: Double?= null,

    @Schema(description = "검색 반경(km)", nullable = true)
     val radiusKm: Double?= null,
) {
    fun applyMapDefaults() = copy(
        mapX = mapX ?: 126.9780,
        mapY = mapY ?: 37.5665,
        radiusKm = radiusKm ?: 10.0
    )
}
