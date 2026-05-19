package com.example.domain.festival.event

import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import com.example.domain.festival.service.FestivalSyncService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

internal class FestivalSyncEventListenerTest {
    private val festivalSyncService = mock(FestivalSyncService::class.java)
    private val listener = FestivalSyncEventListener(festivalSyncService)

    @Test
    @DisplayName("이벤트 수신 시 상세 보강 후 Slack 알림을 수행한다")
    fun handle_success_test() {
        val contentIds = listOf("1001", "1002")
        val listResult = FestivalSyncResultResponse(2, 1, 1, 0, contentIds)
        val event = FestivalSyncCompletedEvent(contentIds, listResult)

        listener.handleFestivalSyncCompleted(event)

        verify(festivalSyncService, times(1))
            .enrichFestivalDetailsAndNotify(contentIds, listResult)
    }

    @Test
    @DisplayName("contentId가 비어있으면 상세 보강을 수행하지 않는다")
    fun handle_empty_test() {
        val listResult = FestivalSyncResultResponse(0, 0, 0, 0, emptyList())
        val event = FestivalSyncCompletedEvent(emptyList(), listResult)

        listener.handleFestivalSyncCompleted(event)

        verifyNoInteractions(festivalSyncService)
    }
}
