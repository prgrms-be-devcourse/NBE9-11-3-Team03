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
        @JvmStatic
        fun from(festivalBookmark: FestivalBookmark): MyBookMarkItemResponse {
            return MyBookMarkItemResponse(
                festivalBookmark.getId(),
                festivalBookmark.getFestival().getId(),
                festivalBookmark.getFestival().getTitle(),
                festivalBookmark.getFestival().getAddress(),
                festivalBookmark.getFestival().getStartDate(),
                festivalBookmark.getFestival().getEndDate(),
                festivalBookmark.getCreatedAt()
            )
        }
    }
}
