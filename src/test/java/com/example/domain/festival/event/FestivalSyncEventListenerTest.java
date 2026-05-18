package com.example.domain.festival.event;

import com.example.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.domain.festival.service.FestivalSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class FestivalSyncEventListenerTest {

    private final FestivalSyncService festivalSyncService = mock(FestivalSyncService.class);

    private final FestivalSyncEventListener listener =
            new FestivalSyncEventListener(festivalSyncService);

    @Test
    @DisplayName("이벤트 수신 시 상세 보강 후 Slack 알림을 수행한다")
    void handle_success_test() {
        List<String> contentIds = List.of("1001", "1002");

        FestivalSyncResultResponse listResult = new FestivalSyncResultResponse(
                2, 1, 1, 0, contentIds
        );

        FestivalSyncCompletedEvent event =
                new FestivalSyncCompletedEvent(contentIds, listResult);

        listener.handleFestivalSyncCompleted(event);

        verify(festivalSyncService, times(1))
                .enrichFestivalDetailsAndNotify(contentIds, listResult);
    }

    @Test
    @DisplayName("contentId가 비어있으면 상세 보강을 수행하지 않는다")
    void handle_empty_test() {
        FestivalSyncResultResponse listResult =
                new FestivalSyncResultResponse(0, 0, 0, 0, List.of());

        FestivalSyncCompletedEvent event =
                new FestivalSyncCompletedEvent(List.of(), listResult);

        listener.handleFestivalSyncCompleted(event);

        verify(festivalSyncService, never())
                .enrichFestivalDetailsAndNotify(anyList(), any());
    }

}
