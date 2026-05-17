package com.example.domain.bookmark.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class FestivalBookmarkResponse(
    val festivalId: Long,
    val memberId: Long,
    @get:JsonProperty("isBookmarked")
    val bookmarked: Boolean,    // ← Boolean을 앞으로
    val bookmarkCount: Int      // ← Int를 뒤로
)