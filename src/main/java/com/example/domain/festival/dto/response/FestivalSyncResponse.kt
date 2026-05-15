package com.example.domain.festival.dto.response

data class FestivalSyncResponse(
    val totalCount: Int,
    val createdCount: Int,
    val updatedCount: Int,
    val failedCount: Int
)
