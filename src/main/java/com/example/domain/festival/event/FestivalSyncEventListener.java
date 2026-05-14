package com.example.domain.festival.event;


import com.example.domain.festival.service.FestivalSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

//축제 목록 동기화 완료 이벤트 리스너
@Component
@RequiredArgsConstructor
public class FestivalSyncEventListener {

    private final FestivalSyncService festivalSyncService;

    //목록 동기화 완료 이벤트를 수신하면, 변경된 contentId에 대해 상세 보강을 수행한다.
    @EventListener
    public void handleFestivalSyncCompleted(FestivalSyncCompletedEvent event) {
        List<String> changedContentIds = event.getChangedContentIds();

        if (changedContentIds == null || changedContentIds.isEmpty()) {
            return;
        }

        festivalSyncService.enrichFestivalDetailsByContentIds(changedContentIds);
    }
}