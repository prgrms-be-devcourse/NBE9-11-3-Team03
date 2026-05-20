package com.example.domain.festival.event

import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

// 축제 목록 동기화 완료 이벤트 발행 클래스
@Component
class FestivalSyncEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    // 목록 동기화 완료 후, 변경된 contentId 목록을 이벤트로 발행한다.
    fun publishSyncCompleted(
        changedContentIds: List<String>,
        listResult: FestivalSyncResultResponse
    ) {
        applicationEventPublisher.publishEvent(
            FestivalSyncCompletedEvent(changedContentIds, listResult)
        )
    }
}