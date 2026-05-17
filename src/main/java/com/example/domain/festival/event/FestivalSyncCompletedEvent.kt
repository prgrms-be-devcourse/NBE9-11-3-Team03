package com.example.domain.festival.event

import com.example.domain.festival.dto.response.FestivalSyncResultResponse

// 목록 동기화 완료 후, 상세 보강이 필요한 contentId 목록을 전달하기 위한 이벤트
data class FestivalSyncCompletedEvent(
    val changedContentIds: List<String>,
    val listResult: FestivalSyncResultResponse
)