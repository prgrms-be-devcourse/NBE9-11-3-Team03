package com.example.domain.festival.event;

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
    @DisplayName("이벤트 수신 시 상세 보강을 수행한다")
    void handle_success_test() {
        // given
        List<String> contentIds = List.of("1001", "1002");
        FestivalSyncCompletedEvent event = new FestivalSyncCompletedEvent(contentIds);

        // when
        listener.handleFestivalSyncCompleted(event);

        // then
        verify(festivalSyncService, times(1))
                .enrichFestivalDetailsByContentIds(contentIds);
    }

    @Test
    @DisplayName("contentId가 비어있으면 상세 보강을 수행하지 않는다")
    void handle_empty_test() {
        // given
        FestivalSyncCompletedEvent event = new FestivalSyncCompletedEvent(List.of());

        // when
        listener.handleFestivalSyncCompleted(event);

        // then
        verify(festivalSyncService, never())
                .enrichFestivalDetailsByContentIds(anyList());
    }

    @Test
    @DisplayName("contentId가 null이면 상세 보강을 수행하지 않는다")
    void handle_null_test() {
        // given
        FestivalSyncCompletedEvent event = new FestivalSyncCompletedEvent(null);

        // when
        listener.handleFestivalSyncCompleted(event);

        // then
        verify(festivalSyncService, never())
                .enrichFestivalDetailsByContentIds(anyList());
    }
}