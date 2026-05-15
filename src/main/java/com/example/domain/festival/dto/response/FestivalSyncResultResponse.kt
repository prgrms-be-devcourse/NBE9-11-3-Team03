package com.example.domain.festival.dto.response

// 동기화 결과 반환용 DTO
data class FestivalSyncResultResponse(
    val totalCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val failedCount: Int,
    val changedContentIds: List<String>
)
