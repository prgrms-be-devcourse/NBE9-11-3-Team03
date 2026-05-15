package com.example.domain.festival.notification

import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import com.example.domain.festival.dto.response.FestivalSyncStatusResponse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class FestivalSyncSlackMessageFactory {
    fun createMessage(
        listResult: FestivalSyncResultResponse,
        detailResult: FestivalSyncResultResponse,
        status: FestivalSyncStatusResponse
    ): String {
        val syncStatus = resolveStatus(listResult, detailResult, status)

        return """
            [축제 데이터 동기화 결과]
            - 종료 시간: ${LocalDateTime.now().format(FORMATTER)}
            - 목록 조회 건수: ${listResult.totalCount}
            - 신규 생성: ${listResult.createdCount}
            - 목록 수정: ${listResult.updatedCount}
            - 목록 실패: ${listResult.failedCount}
            - 상세 보강 대상: ${detailResult.totalCount}
            - 상세 보강 성공/수정: ${detailResult.updatedCount}
            - 상세 보강 실패: ${detailResult.failedCount}
            - Pending 남은 건수: ${status.pendingCount}
            - RATE_LIMIT: ${status.pendingBreakdown.getOrDefault("RATE_LIMIT", 0L)}
            - SERVER_ERROR: ${status.pendingBreakdown.getOrDefault("SERVER_ERROR", 0L)}
            - EXCEPTION: ${status.pendingBreakdown.getOrDefault("EXCEPTION", 0L)}
            - UNPROCESSED: ${status.pendingBreakdown.getOrDefault("UNPROCESSED", 0L)}
            - 상태: $syncStatus
        """.trimIndent()
    }

    private fun resolveStatus(
        listResult: FestivalSyncResultResponse,
        detailResult: FestivalSyncResultResponse,
        status: FestivalSyncStatusResponse
    ): String = when {
        listResult.createdCount == 0 &&
                listResult.updatedCount == 0 &&
                listResult.failedCount == 0 &&
                detailResult.totalCount == 0 &&
                !status.needsRetry -> "변경 없음"

        listResult.failedCount == 0 &&
                detailResult.failedCount == 0 &&
                !status.needsRetry -> "성공"

        listResult.totalCount == 0 &&
                detailResult.totalCount == 0 -> "실패"

        else -> "부분 성공"
    }

    companion object {
        private val FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}