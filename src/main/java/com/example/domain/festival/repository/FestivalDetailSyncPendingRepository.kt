package com.example.domain.festival.repository

import com.example.domain.festival.entity.DetailSyncPendingReason
import com.example.domain.festival.entity.FestivalDetailSyncPending
import org.springframework.data.jpa.repository.JpaRepository

// 상세 보강 재처리 대상 조회/저장/삭제용 Repository
interface FestivalDetailSyncPendingRepository : JpaRepository<FestivalDetailSyncPending, Long> {
    fun findByContentId(contentId: String): FestivalDetailSyncPending?

    fun findAllByOrderByLastFailedAtAsc(): List<FestivalDetailSyncPending>

    fun deleteByContentId(contentId: String)

    fun countByReason(reason: DetailSyncPendingReason): Long
}