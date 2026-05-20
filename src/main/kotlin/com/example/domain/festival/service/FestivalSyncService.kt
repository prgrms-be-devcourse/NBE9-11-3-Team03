package com.example.domain.festival.service

import com.example.domain.festival.client.FestivalApiClient
import com.example.domain.festival.dto.external.FestivalApiItem
import com.example.domain.festival.dto.external.FestivalApiResponse
import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import com.example.domain.festival.entity.DetailSyncPendingReason
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.event.FestivalSyncEventPublisher
import com.example.domain.festival.notification.FestivalSyncSlackMessageFactory
import com.example.domain.festival.repository.FestivalRepository
import com.example.global.notification.SlackNotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException.TooManyRequests
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDateTime

@Service
class FestivalSyncService(
    private val festivalApiClient: FestivalApiClient,
    private val festivalSyncPersistenceService: FestivalSyncPersistenceService,
    private val festivalRepository: FestivalRepository,
    private val festivalSyncEventPublisher: FestivalSyncEventPublisher,
    private val pendingService: FestivalDetailSyncPendingService,
    private val slackNotificationService: SlackNotificationService,
    private val festivalSyncSlackMessageFactory: FestivalSyncSlackMessageFactory
) {
    /** 스케줄러 전용 실행 메서드 */
    fun runScheduledSync(eventStartDate: String, pageNo: Int, numOfRows: Int) {
        log.info(
            "[FestivalScheduler] 축제 동기화 시작 - pageNo={}, numOfRows={}, eventStartDate={}",
            pageNo,
            numOfRows,
            eventStartDate
        )

        try {
            val syncResult = syncFestivalList(pageNo, numOfRows, eventStartDate)

            log.info(
                "[FestivalScheduler] 목록 동기화 완료 - created={}, updated={}, failed={}",
                syncResult.createdCount,
                syncResult.updatedCount,
                syncResult.failedCount
            )

            if (syncResult.totalCount == 0 && syncResult.failedCount > 0) {
                log.warn(
                    "[FestivalScheduler] 목록 동기화 실패로 상세 보강을 건너뜁니다. failed={}",
                    syncResult.failedCount
                )

                notifyFestivalSyncResultOnly(syncResult)
                log.info("[FestivalScheduler] 축제 동기화 종료")
                return
            }

            val detailTargetContentIds =
                collectDetailEnrichTargetContentIds(syncResult.changedContentIds)

            log.info(
                "[FestivalScheduler] 상세 보강 대상 수집 완료 - targetCount={}",
                detailTargetContentIds.size
            )

            enrichFestivalDetailsAndNotify(detailTargetContentIds, syncResult)

            log.info("[FestivalScheduler] 상세 보강 완료")
        } catch (e: Exception) {
            log.error(
                "[FestivalScheduler] 축제 동기화 실패 - message={}",
                e.message,
                e
            )
        }

        log.info("[FestivalScheduler] 축제 동기화 종료")
    }

    // 목록 API 기반 기본 축제 데이터 저장/수정
    fun syncFestivalList(
        pageNo: Int,
        numOfRows: Int,
        eventStartDate: String
    ): FestivalSyncResultResponse {
        val totalStart = System.currentTimeMillis()
        val apiStart = System.currentTimeMillis()

        val response: FestivalApiResponse = try {
            festivalApiClient.fetchFestivalList(pageNo, numOfRows, eventStartDate)
                ?: return FestivalSyncResultResponse(0, 0, 0, 0, emptyList())
        } catch (e: Exception) {
            val apiEnd = System.currentTimeMillis()

            log.error(
                "[FestivalSync] 목록 API 응답 처리 실패 - pageNo={}, numOfRows={}, eventStartDate={}, apiTimeMs={}, message={}",
                pageNo,
                numOfRows,
                eventStartDate,
                apiEnd - apiStart,
                e.message,
                e
            )

            return FestivalSyncResultResponse(0, 0, 0, 1, emptyList())
        }

        val apiEnd = System.currentTimeMillis()

        val items: List<FestivalApiItem> = response.response
            ?.body
            ?.getItems()
            ?.item
            .orEmpty()

        if (items.isEmpty()) {
            return FestivalSyncResultResponse(0, 0, 0, 0, emptyList())
        }

        val dbStart = System.currentTimeMillis()
        val result = festivalSyncPersistenceService.saveListItems(items)
        val dbEnd = System.currentTimeMillis()
        val totalEnd = System.currentTimeMillis()

        log.info(
            "[FestivalSync] 목록 동기화 완료 - total={}, created={}, updated={}, failed={}, changed={}, apiTimeMs={}, dbTimeMs={}, totalTimeMs={}",
            items.size,
            result.createdCount,
            result.updatedCount,
            result.failedCount,
            result.changedContentIds.size,
            apiEnd - apiStart,
            dbEnd - dbStart,
            totalEnd - totalStart
        )

        return result
    }

    // 목록 동기화 완료 후, 변경된 contentId 목록에 대한 상세 보강 이벤트를 발행함
    fun publishSyncCompletedEvent(
        changedContentIds: List<String>,
        listResult: FestivalSyncResultResponse
    ) {
        if (changedContentIds.isEmpty()) {
            return
        }

        festivalSyncEventPublisher.publishSyncCompleted(changedContentIds, listResult)
    }

    // 상세 보강 대상 contentId 수집 (이번 목록 동기화에서 변경된 축제 + 기존 pending 대상)
    @Transactional(readOnly = true)
    fun collectDetailEnrichTargetContentIds(changedContentIds: List<String>): List<String> {
        val targetContentIds = LinkedHashSet(changedContentIds)

        val start = System.currentTimeMillis()

        // 이전 실행에서 실패/미시도된 상세 보강 대상도 함께 재처리
        targetContentIds.addAll(pendingService.findAllContentIds())

        val end = System.currentTimeMillis()
        log.debug("[FestivalSync] pending 조회 완료 - timeMs={}", end - start)

        log.info("[FestivalSync] 상세 보강 대상 수집 완료 - targetCount={}", targetContentIds.size)
        log.debug("[FestivalSync] 상세 보강 대상 목록 - contentIds={}", targetContentIds)

        return targetContentIds.toList()
    }

    // 상세 API 기반 상세 정보 보강 (변경된 contentId 목록만 변경 대상 + 이전 실행에서 실패/미시도된 pending 축제)
    fun enrichFestivalDetailsByContentIds(contentIds: List<String>): FestivalSyncResultResponse {
        var updatedCount = 0
        var failedCount = 0

        val beforePendingCount = pendingService.count()
        val beforePendingFailureCount =
            pendingService.countByReason(DetailSyncPendingReason.RATE_LIMIT) +
                    pendingService.countByReason(DetailSyncPendingReason.SERVER_ERROR) +
                    pendingService.countByReason(DetailSyncPendingReason.EXCEPTION)
        val beforePendingUnprocessedCount =
            pendingService.countByReason(DetailSyncPendingReason.UNPROCESSED)

        var newPendingCount = 0

        val totalStart = System.currentTimeMillis()
        var apiCallCount = 0
        var stopReason: String? = null

        for (i in contentIds.indices) {
            val contentId = contentIds[i]

            try {
                val apiStart = System.currentTimeMillis()
                apiCallCount++

                val detailResponse =
                    festivalApiClient.fetchFestivalDetail(contentId)

                val apiEnd = System.currentTimeMillis()
                log.debug(
                    "[FestivalSync] 상세 API 호출 완료 - contentId={}, timeMs={}",
                    contentId,
                    apiEnd - apiStart
                )

                if (detailResponse?.response?.header?.resultCode != "0000") {
                    failedCount++
                    pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION)
                    newPendingCount++
                    continue
                }

                val detailItems = detailResponse.response
                    ?.body
                    ?.getItems()
                    ?.item
                    .orEmpty()

                if (detailItems.isEmpty()) {
                    failedCount++
                    pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION)
                    newPendingCount++
                    continue
                }

                val detailItem = detailItems[0]

                updatedCount += festivalSyncPersistenceService.updateDetailFields(contentId, detailItem)

                pendingService.remove(contentId)
            } catch (e: TooManyRequests) {
                failedCount++
                stopReason = "429 (quota 초과)"

                pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.RATE_LIMIT)
                newPendingCount++

                for (j in i + 1 until contentIds.size) {
                    pendingService.saveOrUpdate(contentIds[j], DetailSyncPendingReason.UNPROCESSED)
                    newPendingCount++
                }

                log.warn(
                    "[FestivalSync] 외부 API 호출 한도 초과로 상세 보강 중단 - contentId={}, remainingCount={}",
                    contentId,
                    contentIds.size - i - 1
                )

                break
            } catch (e: HttpServerErrorException) {
                failedCount++
                pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.SERVER_ERROR)
                newPendingCount++

                if (stopReason == null) {
                    stopReason = "5xx 서버 오류 (${e.statusCode})"
                }

                log.warn(
                    "[FestivalSync] 외부 API 서버 오류 - contentId={}, status={}",
                    contentId,
                    e.statusCode
                )
            } catch (e: Exception) {
                failedCount++
                pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION)
                newPendingCount++

                log.error(
                    "[FestivalSync] 상세 보강 실패 - contentId={}, message={}",
                    contentId,
                    e.message,
                    e
                )
            }
        }

        val totalEnd = System.currentTimeMillis()
        val afterPendingCount = pendingService.count()

        val totalTargetCount = contentIds.size
        val attemptedCount = apiCallCount
        val skippedCount = totalTargetCount - attemptedCount
        val failureCount = failedCount
        val unprocessedCount = failureCount + skippedCount
        val finalStopReason = stopReason ?: "없음 (정상 처리 또는 일부 실패)"

        log.info(
            "[FestivalSync] 상세 보강 완료 - target={}, updated={}, failed={}, unprocessed={}, pendingBefore={}, pendingFailureBefore={}, pendingUnprocessedBefore={}, pendingAdded={}, pendingAfter={}, attempted={}, skipped={}, stopReason={}, totalTimeMs={}",
            totalTargetCount,
            updatedCount,
            failureCount,
            unprocessedCount,
            beforePendingCount,
            beforePendingFailureCount,
            beforePendingUnprocessedCount,
            newPendingCount,
            afterPendingCount,
            attemptedCount,
            skippedCount,
            finalStopReason,
            totalEnd - totalStart
        )

        return FestivalSyncResultResponse(
            contentIds.size,
            0,
            updatedCount,
            failedCount,
            contentIds
        )
    }

    // 상세 API 기반 상세 정보 보강 (특정 축제 1건)
    fun enrichFestivalDetailByContentId(contentId: String) {
        val response =
            festivalApiClient.fetchFestivalDetail(contentId)

        if (response?.response?.header?.resultCode != "0000") {
            pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION)
            return
        }

        val items = response.response
            ?.body
            ?.getItems()
            ?.item
            .orEmpty()

        if (items.isEmpty()) {
            pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION)
            return
        }

        val detailItem = items[0]

        festivalSyncPersistenceService.updateDetailFields(contentId, detailItem)

        pendingService.remove(contentId)
    }

    @Transactional
    fun updateFestivalStatuses() {
        val now = LocalDateTime.now()

        val endedCount = festivalRepository.updateStatusToEnded(FestivalStatus.ENDED, now)

        val ongoingCount =
            festivalRepository.updateStatusToOngoing(FestivalStatus.ONGOING, FestivalStatus.UPCOMING, now)

        log.info(
            "[FestivalStatus] 상태 업데이트 완료 - ongoing={}, ended={}",
            ongoingCount,
            endedCount
        )
    }

    // 목록 결과만으로 Slack 알림 보내는 메서드
    fun notifyFestivalSyncResultOnly(listResult: FestivalSyncResultResponse) {
        val emptyDetailResult =
            FestivalSyncResultResponse(0, 0, 0, 0, emptyList())

        val status = pendingService.getSyncStatus()

        val message = festivalSyncSlackMessageFactory.createMessage(
            listResult,
            emptyDetailResult,
            status
        )

        slackNotificationService.sendMessage(message)
    }

    // 상세 보강 완료 후 Slack 전송 메서드 (목록 결과까지 포함한 알림)
    fun enrichFestivalDetailsAndNotify(
        contentIds: List<String>,
        listResult: FestivalSyncResultResponse
    ) {
        val detailResult =
            enrichFestivalDetailsByContentIds(contentIds)

        val status =
            pendingService.getSyncStatus()

        val message =
            festivalSyncSlackMessageFactory.createMessage(
                listResult,
                detailResult,
                status
            )

        slackNotificationService.sendMessage(message)
    }

    companion object {
        private val log = LoggerFactory.getLogger(FestivalSyncService::class.java)
    }
}
