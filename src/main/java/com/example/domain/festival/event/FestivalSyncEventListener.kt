package com.example.domain.festival.event

import com.example.domain.festival.service.FestivalSyncService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

// 축제 목록 동기화 완료 이벤트 리스너
@Component
class FestivalSyncEventListener(
    private val festivalSyncService: FestivalSyncService
) {
    // 목록 동기화 완료 이벤트를 수신하면, 변경된 contentId에 대해 상세 보강을 수행한다.
    @Async("festivalDetailTaskExecutor")
    @EventListener
    fun handleFestivalSyncCompleted(event: FestivalSyncCompletedEvent) {
        val changedContentIds = event.changedContentIds

        if (changedContentIds.isNullOrEmpty()) {
            return
        }

        festivalSyncService.enrichFestivalDetailsAndNotify(
            changedContentIds,
            event.listResult
        )
    }
}
