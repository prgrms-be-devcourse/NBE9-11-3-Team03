package com.example.domain.festival.dto.request

import com.example.domain.festival.entity.FestivalStatus
import io.swagger.v3.oas.annotations.media.Schema

@JvmRecord
data class FestivalSearchRequest(
    @Schema(description = "지역 코드 (예: 11)", nullable = true)
    @JvmField val regionCode: String?,

    @Schema(description = "진행 상태 (예: ONGOING)", nullable = true)
    @JvmField val status: FestivalStatus?,

    @Schema(description = "축제 시작 월 (1~12)", nullable = true)
    @JvmField val month: Int?,

    @Schema(description = "검색어", nullable = true)
    @JvmField val keyword: String?,

    @Schema(description = "내 위치 경도", nullable = true)
    @JvmField val mapX: Double?,

    @Schema(description = "내 위치 위도", nullable = true)
    @JvmField val mapY: Double?,

    @Schema(description = "검색 반경(km)", nullable = true)
    @JvmField val radiusKm: Double?
) {
    fun applyMapDefaults() = copy(
        mapX = mapX ?: 126.9780,
        mapY = mapY ?: 37.5665,
        radiusKm = radiusKm ?: 10.0
    )
}