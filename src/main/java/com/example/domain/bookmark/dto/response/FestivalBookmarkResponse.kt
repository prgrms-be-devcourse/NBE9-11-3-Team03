package com.example.domain.bookmark.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

class FestivalBookmarkResponse(
    val festivalId: Long,
    val memberId: Long,
    @field:JsonProperty("isBookmarked")
    val isBookmarked: Boolean,
    val bookmarkCount: Int
)
