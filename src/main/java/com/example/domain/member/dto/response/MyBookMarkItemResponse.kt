package com.example.domain.member.dto.response

import com.example.domain.bookmark.entity.FestivalBookmark
import java.time.LocalDateTime

data class MyBookMarkItemResponse(
    val bookmarkId: Long,
    val festivalId: Long,
    val title: String,
    val address: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val bookmarkedAt: LocalDateTime

) {
    companion object {
        fun from(festivalBookmark: FestivalBookmark): MyBookMarkItemResponse {
            return MyBookMarkItemResponse(
                festivalBookmark.id,
                festivalBookmark.festival.id,
                festivalBookmark.festival.title,
                festivalBookmark.festival.address,
                festivalBookmark.festival.startDate,
                festivalBookmark.festival.endDate,
                festivalBookmark.createdAt
            )
        }
    }
}
