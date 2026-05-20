package com.example.domain.festival.dto.response

import org.springframework.data.domain.Page

data class FestivalPageResponse<T>(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val content: List<T>,
) {
    companion object {
        fun <T> from(page: Page<T>) = FestivalPageResponse(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            content = page.content,
        )
    }
}
