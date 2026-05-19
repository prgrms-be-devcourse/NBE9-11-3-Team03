package com.example.domain.festival.service

import com.example.domain.festival.converter.FestivalApiConverter
import com.example.domain.festival.dto.external.FestivalApiItem
import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import com.example.domain.festival.repository.FestivalRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.NoSuchElementException

// 축제 동기화 중 DB 조회/저장/수정만 짧은 트랜잭션으로 처리하는 서비스
@Service
class FestivalSyncPersistenceService(
    private val festivalApiConverter: FestivalApiConverter,
    private val festivalRepository: FestivalRepository
) {
    @Transactional
    fun saveListItems(items: List<FestivalApiItem>): FestivalSyncResultResponse {
        var createdCount = 0
        var updatedCount = 0
        var failedCount = 0
        val changedContentIds = mutableListOf<String>()

        val contentIds = items
            .mapNotNull { it.contentid }
            .distinct()

        val existingFestivalMap = festivalRepository.findAllByContentIdIn(contentIds)
            .associateBy { it.contentId }

        for (item in items) {
            try {
                val contentId = item.contentid
                val existingFestival = existingFestivalMap[contentId]

                if (existingFestival == null) {
                    val newFestival = festivalApiConverter.toEntityFromListItem(item)
                    festivalRepository.save(newFestival)
                    createdCount++
                    changedContentIds.add(requireNotNull(contentId))
                } else if (festivalApiConverter.hasListChanges(existingFestival, item)) {
                    festivalApiConverter.updateFromListItem(existingFestival, item)
                    updatedCount++
                    changedContentIds.add(requireNotNull(contentId))
                }
            } catch (e: Exception) {
                failedCount++
                log.warn(
                    "[FestivalSync] 목록 동기화 항목 실패 - contentId={}, message={}",
                    item.contentid,
                    e.message
                )
            }
        }

        return FestivalSyncResultResponse(
            items.size,
            createdCount,
            updatedCount,
            failedCount,
            changedContentIds
        )
    }

    @Transactional
    fun updateDetailFields(contentId: String, detailItem: FestivalApiItem): Int {
        val festival = festivalRepository.findByContentId(contentId)
            .orElseThrow {
                NoSuchElementException("해당 contentId의 축제를 찾을 수 없습니다. contentId=$contentId")
            }

        val wasDetailIncomplete = festivalApiConverter.isDetailIncomplete(festival)

        if (wasDetailIncomplete || festivalApiConverter.hasDetailChanges(festival, detailItem)) {
            festivalApiConverter.updateDetailFields(festival, detailItem)

            if (!wasDetailIncomplete) {
                return 1
            }
        }

        return 0
    }

    companion object {
        private val log = LoggerFactory.getLogger(FestivalSyncPersistenceService::class.java)
    }
}
