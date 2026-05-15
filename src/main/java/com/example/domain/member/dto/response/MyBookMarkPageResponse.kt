package com.example.domain.member.dto.response

import com.example.domain.bookmark.entity.FestivalBookmark
import org.springframework.data.domain.Page

data class MyBookMarkPageResponse(
    val content: List<MyBookMarkItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
) {
    companion object {
        @JvmStatic
        fun from(bookmarks: Page<FestivalBookmark>) = MyBookMarkPageResponse(
            content = bookmarks.content.map(MyBookMarkItemResponse::from),
            page = bookmarks.number,
            size = bookmarks.size,
            totalElements = bookmarks.totalElements,
            totalPages = bookmarks.totalPages,
            hasNext = bookmarks.hasNext()
        )
    }
}