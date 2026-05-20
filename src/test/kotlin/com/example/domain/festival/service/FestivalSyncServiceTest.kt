package com.example.domain.festival.service

import com.example.domain.festival.client.FestivalApiClient
import com.example.domain.festival.dto.external.FestivalApiBody
import com.example.domain.festival.dto.external.FestivalApiHeader
import com.example.domain.festival.dto.external.FestivalApiItem
import com.example.domain.festival.dto.external.FestivalApiItems
import com.example.domain.festival.dto.external.FestivalApiResponse
import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import com.example.domain.festival.dto.response.FestivalSyncStatusResponse
import com.example.domain.festival.entity.DetailSyncPendingReason
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.event.FestivalSyncEventPublisher
import com.example.domain.festival.notification.FestivalSyncSlackMessageFactory
import com.example.domain.festival.repository.FestivalRepository
import com.example.global.notification.SlackNotificationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

internal class FestivalSyncServiceTest {
    private val festivalApiClient = mock(FestivalApiClient::class.java)
    private val festivalSyncPersistenceService = mock(FestivalSyncPersistenceService::class.java)
    private val festivalRepository = mock(FestivalRepository::class.java)
    private val festivalSyncEventPublisher = mock(FestivalSyncEventPublisher::class.java)
    private val pendingService = mock(FestivalDetailSyncPendingService::class.java)
    private val slackNotificationService = mock(SlackNotificationService::class.java)
    private val festivalSyncSlackMessageFactory = mock(FestivalSyncSlackMessageFactory::class.java)

    private val festivalSyncService = FestivalSyncService(
        festivalApiClient,
        festivalSyncPersistenceService,
        festivalRepository,
        festivalSyncEventPublisher,
        pendingService,
        slackNotificationService,
        festivalSyncSlackMessageFactory
    )

    @Nested
    @DisplayName("목록 동기화 테스트")
    internal inner class SyncFestivalListTest {
        @Test
        @DisplayName("목록 API 응답을 받은 뒤 DB 반영 서비스에 저장을 위임한다")
        fun syncFestivalList_success_test() {
            val item = createApiItem("1001", "가야문화축제")
            val response = createResponse(listOf(item))
            val expectedResult = FestivalSyncResultResponse(1, 1, 0, 0, listOf("1001"))

            given(festivalApiClient.fetchFestivalList(1, 10, "20260101"))
                .willReturn(response)
            given(festivalSyncPersistenceService.saveListItems(listOf(item)))
                .willReturn(expectedResult)

            val result = festivalSyncService.syncFestivalList(1, 10, "20260101")

            assertThat(result).isEqualTo(expectedResult)
            verify(festivalApiClient).fetchFestivalList(1, 10, "20260101")
            verify(festivalSyncPersistenceService).saveListItems(listOf(item))
        }

        @Test
        @DisplayName("목록 API 응답이 비어 있으면 DB 반영을 수행하지 않는다")
        fun syncFestivalList_empty_response_test() {
            val response = createResponse(emptyList())

            given(festivalApiClient.fetchFestivalList(1, 10, "20260101"))
                .willReturn(response)

            val result = festivalSyncService.syncFestivalList(1, 10, "20260101")

            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.failedCount).isEqualTo(0)
            assertThat(result.changedContentIds).isEmpty()
            verify(festivalSyncPersistenceService, never()).saveListItems(emptyList())
        }

        @Test
        @DisplayName("목록 API 호출 자체가 실패하면 실패 결과를 반환하고 DB 반영을 수행하지 않는다")
        fun syncFestivalList_api_failure_test() {
            given(festivalApiClient.fetchFestivalList(1, 10, "20260101"))
                .willThrow(RuntimeException("API 실패"))

            val result = festivalSyncService.syncFestivalList(1, 10, "20260101")

            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.failedCount).isEqualTo(1)
            assertThat(result.changedContentIds).isEmpty()
            verify(festivalSyncPersistenceService, never()).saveListItems(emptyList())
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    internal inner class PublishSyncCompletedEventTest {
        @Test
        @DisplayName("변경된 contentId가 있으면 목록 결과와 함께 동기화 완료 이벤트를 발행한다")
        fun publishSyncCompletedEvent_success_test() {
            val changedContentIds = listOf("1001", "1002")
            val listResult = FestivalSyncResultResponse(2, 1, 1, 0, changedContentIds)

            festivalSyncService.publishSyncCompletedEvent(changedContentIds, listResult)

            verify(festivalSyncEventPublisher, times(1))
                .publishSyncCompleted(changedContentIds, listResult)
        }

        @Test
        @DisplayName("변경된 contentId가 없으면 동기화 완료 이벤트를 발행하지 않는다")
        fun publishSyncCompletedEvent_empty_test() {
            val listResult = FestivalSyncResultResponse(0, 0, 0, 0, emptyList())

            festivalSyncService.publishSyncCompletedEvent(emptyList(), listResult)

            verify(festivalSyncEventPublisher, never())
                .publishSyncCompleted(emptyList(), listResult)
        }
    }

    @Test
    @DisplayName("상세 보강 대상이 없으면 목록 결과만으로 Slack 알림을 전송한다")
    fun notifyFestivalSyncResultOnly_test() {
        val listResult = FestivalSyncResultResponse(200, 0, 0, 0, emptyList())
        val emptyDetailResult = FestivalSyncResultResponse(0, 0, 0, 0, emptyList())
        val status = createSyncStatus()
        val message = "[축제 데이터 동기화 결과] 변경 없음"

        given(pendingService.getSyncStatus()).willReturn(status)
        given(festivalSyncSlackMessageFactory.createMessage(listResult, emptyDetailResult, status))
            .willReturn(message)

        festivalSyncService.notifyFestivalSyncResultOnly(listResult)

        verify(pendingService, times(1)).getSyncStatus()
        verify(festivalSyncSlackMessageFactory, times(1))
            .createMessage(listResult, emptyDetailResult, status)
        verify(slackNotificationService, times(1)).sendMessage(message)
    }

    @Test
    @DisplayName("상세 보강 완료 후 목록 결과와 상세 결과를 포함해 Slack 알림을 전송한다")
    fun enrichFestivalDetailsAndNotify_test() {
        val contentIds = emptyList<String>()
        val listResult = FestivalSyncResultResponse(200, 3, 2, 0, listOf("1001", "1002"))
        val detailResult = FestivalSyncResultResponse(0, 0, 0, 0, emptyList())
        val status = createSyncStatus()
        val message = "[축제 데이터 동기화 결과] 성공"

        given(pendingService.getSyncStatus()).willReturn(status)
        given(festivalSyncSlackMessageFactory.createMessage(listResult, detailResult, status))
            .willReturn(message)

        festivalSyncService.enrichFestivalDetailsAndNotify(contentIds, listResult)

        verify(pendingService, times(1)).getSyncStatus()
        verify(festivalSyncSlackMessageFactory, times(1))
            .createMessage(listResult, detailResult, status)
        verify(slackNotificationService, times(1)).sendMessage(message)
    }

    @Nested
    @DisplayName("상세 보강 예외 정책 테스트")
    internal inner class EnrichFestivalDetailsFailureTest {
        @Test
        @DisplayName("상세 보강 중 429가 발생하면 현재 건과 뒤 미시도 건을 pending에 저장하고 즉시 중단한다")
        fun enrichFestivalDetailsByContentIds_429_break_test() {
            given(festivalApiClient.fetchFestivalDetail("1001"))
                .willThrow(
                    HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        HttpHeaders.EMPTY,
                        ByteArray(0),
                        null
                    )
                )

            val result = festivalSyncService.enrichFestivalDetailsByContentIds(listOf("1001", "1002"))

            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.updatedCount).isEqualTo(0)
            assertThat(result.failedCount).isEqualTo(1)

            verify(festivalApiClient).fetchFestivalDetail("1001")
            verify(festivalApiClient, never()).fetchFestivalDetail("1002")
            verify(pendingService).saveOrUpdate("1001", DetailSyncPendingReason.RATE_LIMIT)
            verify(pendingService).saveOrUpdate("1002", DetailSyncPendingReason.UNPROCESSED)
        }

        @Test
        @DisplayName("상세 보강 중 5xx가 발생하면 해당 건을 pending에 저장하고 다음 건으로 진행한다")
        fun enrichFestivalDetailsByContentIds_5xx_continue_test() {
            val detailItem = createApiItem("1002", "축제2").apply {
                overview = "새 상세 설명"
                homepage = "https://test.com"
            }
            val detailResponse = createResponse(listOf(detailItem))

            given(festivalApiClient.fetchFestivalDetail("1001"))
                .willThrow(HttpServerErrorException(HttpStatus.BAD_GATEWAY))
            given(festivalApiClient.fetchFestivalDetail("1002"))
                .willReturn(detailResponse)
            given(festivalSyncPersistenceService.updateDetailFields("1002", detailItem))
                .willReturn(1)

            val result = festivalSyncService.enrichFestivalDetailsByContentIds(listOf("1001", "1002"))

            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.updatedCount).isEqualTo(1)
            assertThat(result.failedCount).isEqualTo(1)

            verify(pendingService).saveOrUpdate("1001", DetailSyncPendingReason.SERVER_ERROR)
            verify(festivalSyncPersistenceService).updateDetailFields("1002", detailItem)
            verify(pendingService).remove("1002")
        }

        @Test
        @DisplayName("상세 응답 구조가 비정상이면 pending에 저장한다")
        fun enrichFestivalDetailsByContentIds_invalid_response_save_pending_test() {
            given(festivalApiClient.fetchFestivalDetail("1001")).willReturn(null)

            val result = festivalSyncService.enrichFestivalDetailsByContentIds(listOf("1001"))

            assertThat(result.updatedCount).isEqualTo(0)
            assertThat(result.failedCount).isEqualTo(1)

            verify(pendingService).saveOrUpdate("1001", DetailSyncPendingReason.EXCEPTION)
            verify(festivalSyncPersistenceService, never()).updateDetailFields("1001", createApiItem("1001", "축제1"))
            verify(pendingService, never()).remove("1001")
        }
    }

    private fun createApiItem(
        contentId: String,
        title: String
    ): FestivalApiItem =
        FestivalApiItem(
            contentid = contentId,
            title = title,
            tel = "055-330-6840",
            addr1 = "경상남도 김해시",
            addr2 = "대성동",
            homepage = "https://test.com",
            overview = "상세 설명 없음",
            firstimage = "image1.jpg",
            firstimage2 = "image2.jpg",
            mapx = "128.87",
            mapy = "35.23",
            lDongRegnCd = "48",
            eventstartdate = "20260430",
            eventenddate = "20260503"
        )

    private fun createResponse(itemList: List<FestivalApiItem>): FestivalApiResponse {
        val body = FestivalApiBody().apply {
            setPrivateItems(FestivalApiItems(itemList))
            numOfRows = itemList.size
            pageNo = 1
            totalCount = itemList.size
        }

        return FestivalApiResponse(
            response = FestivalApiResponse.Response(
                header = FestivalApiHeader(
                    resultCode = "0000",
                    resultMsg = "OK"
                ),
                body = body
            )
        )
    }

    private fun FestivalApiBody.setPrivateItems(items: FestivalApiItems) {
        val field = javaClass.getDeclaredField("items")
        field.isAccessible = true
        field.set(this, items)
    }

    private fun createSyncStatus(): FestivalSyncStatusResponse =
        FestivalSyncStatusResponse(
            pendingCount = 0L,
            pendingBreakdown = mapOf(
                "RATE_LIMIT" to 0L,
                "SERVER_ERROR" to 0L,
                "EXCEPTION" to 0L,
                "UNPROCESSED" to 0L
            ),
            needsRetry = false
        )
}
