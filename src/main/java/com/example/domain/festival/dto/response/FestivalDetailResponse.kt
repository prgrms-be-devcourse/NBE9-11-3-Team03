package com.example.domain.festival.dto.response

import com.example.domain.festival.entity.Festival
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class FestivalDetailResponse(
    val id: Long,
    val title: String,
    val firstImageUrl: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val address: String,
    val contactNumber: String?,
    val homepageUrl: String?,
    val status: String,
    val overview: String,
    val mapX: Double,
    val mapY: Double,
    val viewCount: Int,
    val bookMarkCount: Int,
    val averageRate: Double,
    @get:JsonProperty("isBookmarked")
    val isBookmarked: Boolean,
) {
    companion object {
        fun from(festival: Festival, isBookmarked: Boolean) = FestivalDetailResponse(
            id = festival.id,
            title = festival.title,
            firstImageUrl = festival.firstImageUrl,
            startDate = festival.startDate.toLocalDate(),
            endDate = festival.endDate.toLocalDate(),
            address = festival.address,
            contactNumber = festival.contactNumber,
            homepageUrl = festival.homepageUrl,
            status = festival.status.name,
            overview = festival.overview,
            mapX = festival.mapX,
            mapY = festival.mapY,
            viewCount = festival.viewCount,
            bookMarkCount = festival.bookMarkCount,
            averageRate = festival.averageRate,
            isBookmarked = isBookmarked,
        )
    }
}
