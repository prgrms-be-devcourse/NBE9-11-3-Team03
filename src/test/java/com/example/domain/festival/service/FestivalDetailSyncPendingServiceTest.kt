package com.example.domain.festival.service

import com.example.domain.festival.entity.DetailSyncPendingReason
import com.example.domain.festival.entity.FestivalDetailSyncPending.Companion.create
import com.example.domain.festival.repository.FestivalDetailSyncPendingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*

internal class FestivalDetailSyncPendingServiceTest {
    private val repository = mock(FestivalDetailSyncPendingRepository::class.java)
    private val service = FestivalDetailSyncPendingService(repository)

    @Test
    @DisplayName("pending이 없으면 새로 생성한다")
    fun saveOrUpdate_create_test() {
        given(repository.findByContentId("1001")).willReturn(null)

        service.saveOrUpdate("1001", DetailSyncPendingReason.EXCEPTION)

        verify(repository).save(any())
    }

    @Test
    @DisplayName("pending이 이미 존재하면 retryCount 증가 및 갱신한다")
    fun saveOrUpdate_update_test() {
        val existing = create("1001", DetailSyncPendingReason.EXCEPTION)

        given(repository.findByContentId("1001")).willReturn(existing)

        service.saveOrUpdate("1001", DetailSyncPendingReason.SERVER_ERROR)

        verify(repository, never()).save(any())
        assertThat(existing.retryCount).isEqualTo(2)
        assertThat(existing.reason).isEqualTo(DetailSyncPendingReason.SERVER_ERROR)
    }

    @Test
    @DisplayName("성공 시 pending을 제거한다")
    fun remove_test() {
        service.remove("1001")

        verify(repository).deleteByContentId("1001")
    }

    @Test
    @DisplayName("pending 전체 조회 시 contentId 리스트로 반환한다")
    fun findAllContentIds_test() {
        val p1 = create("1001", DetailSyncPendingReason.EXCEPTION)
        val p2 = create("1002", DetailSyncPendingReason.SERVER_ERROR)

        given(repository.findAllByOrderByLastFailedAtAsc()).willReturn(listOf(p1, p2))

        val result = service.findAllContentIds()

        assertThat(result).containsExactly("1001", "1002")
    }

    @Test
    @DisplayName("sync-status 조회 시 pending이 없으면 needsRetry는 false이다")
    fun getSyncStatus_empty_test() {
        given(repository.count()).willReturn(0L)
        given(repository.countByReason(DetailSyncPendingReason.RATE_LIMIT)).willReturn(0L)
        given(repository.countByReason(DetailSyncPendingReason.SERVER_ERROR)).willReturn(0L)
        given(repository.countByReason(DetailSyncPendingReason.EXCEPTION)).willReturn(0L)
        given(repository.countByReason(DetailSyncPendingReason.UNPROCESSED)).willReturn(0L)

        val result = service.getSyncStatus()

        assertThat(result.pendingCount).isEqualTo(0L)
        assertThat(result.needsRetry).isFalse()
        assertThat(result.pendingBreakdown).containsEntry("RATE_LIMIT", 0L)
        assertThat(result.pendingBreakdown).containsEntry("SERVER_ERROR", 0L)
        assertThat(result.pendingBreakdown).containsEntry("EXCEPTION", 0L)
        assertThat(result.pendingBreakdown).containsEntry("UNPROCESSED", 0L)
    }

    @Test
    @DisplayName("sync-status 조회 시 pending breakdown과 needsRetry를 반환한다")
    fun getSyncStatus_with_pending_test() {
        given(repository.count()).willReturn(3L)
        given(repository.countByReason(DetailSyncPendingReason.RATE_LIMIT)).willReturn(1L)
        given(repository.countByReason(DetailSyncPendingReason.SERVER_ERROR)).willReturn(0L)
        given(repository.countByReason(DetailSyncPendingReason.EXCEPTION)).willReturn(0L)
        given(repository.countByReason(DetailSyncPendingReason.UNPROCESSED)).willReturn(2L)

        val result = service.getSyncStatus()

        assertThat(result.pendingCount).isEqualTo(3L)
        assertThat(result.needsRetry).isTrue()
        assertThat(result.pendingBreakdown).containsEntry("RATE_LIMIT", 1L)
        assertThat(result.pendingBreakdown).containsEntry("SERVER_ERROR", 0L)
        assertThat(result.pendingBreakdown).containsEntry("EXCEPTION", 0L)
        assertThat(result.pendingBreakdown).containsEntry("UNPROCESSED", 2L)
    }
}
