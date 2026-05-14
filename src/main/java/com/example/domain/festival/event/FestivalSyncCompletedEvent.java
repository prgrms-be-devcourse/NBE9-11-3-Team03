package com.example.domain.festival.event;

import lombok.Getter;

import java.util.List;

//목록 동기화 완료 후, 상세 보강이 필요한 contentId 목록을 전달하기 위한 이벤트
@Getter
public class FestivalSyncCompletedEvent {

    private final List<String> changedContentIds;

    public FestivalSyncCompletedEvent(List<String> changedContentIds) {
        this.changedContentIds = changedContentIds;
    }
}