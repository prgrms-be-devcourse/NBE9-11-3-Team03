package com.example.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;


//축제 상세 동기화 상태 조회 응답 DTO
//정보 제공: 현재 pending 전체 건수, reason별 pending 건수, 재처리 필요 여부
@Getter
@AllArgsConstructor
public class FestivalSyncStatusResponseDto {

    private long pendingCount;

    // reason별 pending 건수
    private Map<String, Long> pendingBreakdown;

    private boolean needsRetry;
}