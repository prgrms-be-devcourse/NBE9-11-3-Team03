package com.example.domain.festival.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;


//축제 목록 동기화 완료 이벤트 발행 클래스
@Component
@RequiredArgsConstructor
public class FestivalSyncEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    //목록 동기화 완료 후, 변경된 contentId 목록을 이벤트로 발행한다.
    public void publishSyncCompleted(List<String> changedContentIds) {
        applicationEventPublisher.publishEvent(new FestivalSyncCompletedEvent(changedContentIds));
    }
}
