package com.example.domain.festival.dto.response

import com.example.domain.festival.entity.Festival

data class FestivalMarkerResponse(
    val id: Long,
    val title: String,
    val mapX: Double,
    val mapY: Double,
) {
    companion object {
        fun from(festival: Festival) = FestivalMarkerResponse(
            id = festival.id,
            title = festival.title,
            mapX = festival.mapX,
            mapY = festival.mapY,

            )

    }
}
