package com.example.domain.festival.dto.response

import com.example.domain.festival.entity.Festival
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class FestivalListResponse(
    val id: Long,
    val title: String,
    val thumbnail: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val address: String,
    val status: String,
    val viewCount: Int,
    val bookMarkCount: Int,
    val averageRate: Double,
    @get:JsonProperty("isBookmarked")
    val isBookmarked: Boolean,
) {
    companion object {
        @JvmStatic
        fun from(festival: Festival, isBookmarked: Boolean): FestivalListResponse {
            return FestivalListResponse(
                id = festival.id,
                title = festival.title,
                thumbnail = festival.thumbnailUrl,
                startDate = festival.startDate.toLocalDate(),
                endDate = festival.endDate.toLocalDate(),
                address = festival.address,
                status = festival.status.name,
                viewCount = festival.viewCount,
                bookMarkCount = festival.bookMarkCount,
                averageRate = festival.averageRate,
                isBookmarked = isBookmarked,
            )
        }
    }
}
