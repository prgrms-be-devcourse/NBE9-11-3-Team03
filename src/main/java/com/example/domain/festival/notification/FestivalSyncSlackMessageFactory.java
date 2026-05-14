package com.example.domain.festival.notification;

import com.example.domain.festival.dto.response.FestivalSyncResult;
import com.example.domain.festival.dto.response.FestivalSyncStatusResponseDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FestivalSyncSlackMessageFactory {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String createMessage(
            FestivalSyncResult listResult,
            FestivalSyncResult detailResult,
            FestivalSyncStatusResponseDto status
    ) {
        String syncStatus = resolveStatus(listResult, detailResult, status);

        return """
                [축제 데이터 동기화 결과]
                - 종료 시간: %s
                - 목록 조회 건수: %d
                - 신규 생성: %d
                - 목록 수정: %d
                - 목록 실패: %d
                - 상세 보강 대상: %d
                - 상세 보강 성공/수정: %d
                - 상세 보강 실패: %d
                - Pending 남은 건수: %d
                - RATE_LIMIT: %d
                - SERVER_ERROR: %d
                - EXCEPTION: %d
                - UNPROCESSED: %d
                - 상태: %s
                """.formatted(
                LocalDateTime.now().format(FORMATTER),
                listResult.getTotalCount(),
                listResult.getCreatedCount(),
                listResult.getUpdatedCount(),
                listResult.getFailedCount(),
                detailResult.getTotalCount(),
                detailResult.getUpdatedCount(),
                detailResult.getFailedCount(),
                status.getPendingCount(),
                status.getPendingBreakdown().getOrDefault("RATE_LIMIT", 0L),
                status.getPendingBreakdown().getOrDefault("SERVER_ERROR", 0L),
                status.getPendingBreakdown().getOrDefault("EXCEPTION", 0L),
                status.getPendingBreakdown().getOrDefault("UNPROCESSED", 0L),
                syncStatus
        );
    }

    private String resolveStatus(
            FestivalSyncResult listResult,
            FestivalSyncResult detailResult,
            FestivalSyncStatusResponseDto status
    ) {
        if (listResult.getCreatedCount() == 0
                && listResult.getUpdatedCount() == 0
                && listResult.getFailedCount() == 0
                && detailResult.getTotalCount() == 0
                && !status.isNeedsRetry()) {
            return "변경 없음";
        }

        if (listResult.getFailedCount() == 0
                && detailResult.getFailedCount() == 0
                && !status.isNeedsRetry()) {
            return "성공";
        }

        if (listResult.getTotalCount() == 0 && detailResult.getTotalCount() == 0) {
            return "실패";
        }

        return "부분 성공";

    }
}
