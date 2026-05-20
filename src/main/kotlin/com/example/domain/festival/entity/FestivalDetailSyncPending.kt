package com.example.domain.festival.entity

import com.example.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

// 상세 보강 실패/미시도 대상을 관리하는 전용 테이블, 다음 동기화 실행 시 재처리 대상 제공 위함
// 재처리 상태는 별도 테이블로 분리하여, 축제의 본 데이터와의 책임을 명확히 하기 위함
@Entity
@Table(
    name = "festival_detail_sync_pending",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_pending_content_id", columnNames = ["content_id"])
    ]
)
class FestivalDetailSyncPending protected constructor(
    @Column(name = "content_id", nullable = false, unique = true)
    var contentId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var reason: DetailSyncPendingReason,

    @Column(nullable = false)
    var retryCount: Int = 1,

    @Column(nullable = false)
    var lastFailedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity() {

    // 이미 pending에 있는 대상을 다시 실패 처리할 때 갱신
    fun updateFailure(reason: DetailSyncPendingReason) {
        this.reason = reason
        retryCount++
        lastFailedAt = LocalDateTime.now()
    }

    companion object {
        // pending 생성 팩토리 메서드
        fun create(
            contentId: String,
            reason: DetailSyncPendingReason
        ): FestivalDetailSyncPending =
            FestivalDetailSyncPending(
                contentId = contentId,
                reason = reason,
                retryCount = 1,
                lastFailedAt = LocalDateTime.now()
            )
    }
}