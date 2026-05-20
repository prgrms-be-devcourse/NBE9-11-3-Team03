package com.example.domain.festival.service

import com.example.domain.festival.dto.response.FestivalSyncStatusResponse
import com.example.domain.festival.entity.DetailSyncPendingReason
import com.example.domain.festival.entity.FestivalDetailSyncPending
import com.example.domain.festival.repository.FestivalDetailSyncPendingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 상세 보강 재처리 대상 저장/조회/삭제를 담당하는 서비스
@Service
@Transactional
class FestivalDetailSyncPendingService(
    private val pendingRepository: FestivalDetailSyncPendingRepository
) {
    // 실패 또는 미시도 대상 contentId를 pending에 저장한다.
    fun saveOrUpdate(contentId: String, reason: DetailSyncPendingReason) {
        val pending = pendingRepository.findByContentId(contentId)

        if (pending != null) {
            pending.updateFailure(reason)
            return
        }

        pendingRepository.save(
            FestivalDetailSyncPending.create(contentId, reason)
        )
    }

    // 상세 보강 성공 시 pending에서 제거한다.
    fun remove(contentId: String) {
        pendingRepository.deleteByContentId(contentId)
    }

    // 재처리 대상 contentId 전체 조회 (오래된 실패부터 다시 처리할 수 있도록 lastFailedAt 오름차순 기준 처리)
    @Transactional(readOnly = true)
    fun findAllContentIds(): List<String> =
        pendingRepository.findAllByOrderByLastFailedAtAsc()
            .map { it.contentId }

    // 재처리 대상 건수 카운터(로그용)
    @Transactional(readOnly = true)
    fun count(): Long =
        pendingRepository.count()

    // reason별 pending 건수 조회(로그용)
    @Transactional(readOnly = true)
    fun countByReason(reason: DetailSyncPendingReason): Long =
        pendingRepository.countByReason(reason)

    // reason별 pending 건수 조회(로그용)
    @Transactional(readOnly = true)
    fun getSyncStatus(): FestivalSyncStatusResponse {
        val pendingBreakdown = linkedMapOf<String, Long>()

        for (reason in DetailSyncPendingReason.entries) {
            pendingBreakdown[reason.name] = countByReason(reason)
        }

        val pendingCount = count()

        return FestivalSyncStatusResponse(
            pendingCount,
            pendingBreakdown,
            pendingCount > 0
        )
    }
}
