package com.example.domain.festival.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

// 축제 상세 동기화 상태 조회 응답 DTO
// 정보 제공: 현재 pending 전체 건수, reason별 pending 건수, 재처리 필요 여부
data class FestivalSyncStatusResponse(
    val pendingCount: Long,
    val pendingBreakdown: Map<String, Long>,
    @get:JsonProperty("needsRetry")
    @get:JvmName("isNeedsRetry")
    val needsRetry: Boolean
)
