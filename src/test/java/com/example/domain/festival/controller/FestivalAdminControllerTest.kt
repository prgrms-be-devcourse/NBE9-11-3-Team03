package com.example.domain.festival.controller

import com.example.domain.festival.dto.response.FestivalSyncResultResponse
import com.example.domain.festival.dto.response.FestivalSyncStatusResponse
import com.example.domain.festival.service.FestivalDetailSyncPendingService
import com.example.domain.festival.service.FestivalSyncService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

internal class FestivalAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var festivalSyncService: FestivalSyncService
    private lateinit var pendingService: FestivalDetailSyncPendingService

    @BeforeEach
    fun setUp() {
        festivalSyncService = mock(FestivalSyncService::class.java)
        pendingService = mock(FestivalDetailSyncPendingService::class.java)

        val controller = FestivalAdminController(festivalSyncService, pendingService)

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    @DisplayName("sync-status 조회 시 재처리 대상이 없으면 정상 상태 메시지를 반환한다")
    fun getFestivalSyncStatus_no_retry_test() {
        val response = FestivalSyncStatusResponse(
            pendingCount = 0L,
            pendingBreakdown = mapOf(
                "RATE_LIMIT" to 0L,
                "SERVER_ERROR" to 0L,
                "EXCEPTION" to 0L,
                "UNPROCESSED" to 0L
            ),
            needsRetry = false
        )

        given(pendingService.getSyncStatus()).willReturn(response)

        mockMvc.perform(get("/api/admin/festivals/sync-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("축제 동기화가 정상 상태입니다. 재처리할 상세 대상이 없습니다."))
            .andExpect(jsonPath("$.data.pendingCount").value(0))
            .andExpect(jsonPath("$.data.needsRetry").value(false))
            .andExpect(jsonPath("$.data.pendingBreakdown.RATE_LIMIT").value(0))
            .andExpect(jsonPath("$.data.pendingBreakdown.UNPROCESSED").value(0))
    }

    @Test
    @DisplayName("sync-status 조회 시 RATE_LIMIT 대상이 있으면 API 호출 제한 메시지를 반환한다")
    fun getFestivalSyncStatus_need_retry_test() {
        val response = FestivalSyncStatusResponse(
            pendingCount = 3L,
            pendingBreakdown = mapOf(
                "RATE_LIMIT" to 1L,
                "SERVER_ERROR" to 0L,
                "EXCEPTION" to 0L,
                "UNPROCESSED" to 2L
            ),
            needsRetry = true
        )

        given(pendingService.getSyncStatus()).willReturn(response)

        mockMvc.perform(get("/api/admin/festivals/sync-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("API 호출 제한으로 인해 상세 동기화 재처리가 필요합니다."))
            .andExpect(jsonPath("$.data.pendingCount").value(3))
            .andExpect(jsonPath("$.data.needsRetry").value(true))
            .andExpect(jsonPath("$.data.pendingBreakdown.RATE_LIMIT").value(1))
            .andExpect(jsonPath("$.data.pendingBreakdown.UNPROCESSED").value(2))
    }

    @Test
    @DisplayName("sync-and-enrich 호출 시 목록 동기화 결과를 반환하고 상세 보강 이벤트를 발행한다")
    fun syncAndEnrichFestivals_success_test() {
        val changedContentIds = listOf("1001", "1002")
        val listResult = FestivalSyncResultResponse(
            totalCount = 2,
            createdCount = 1,
            updatedCount = 1,
            failedCount = 0,
            changedContentIds = changedContentIds
        )

        given(festivalSyncService.syncFestivalList(1, 200, "20260101"))
            .willReturn(listResult)
        given(festivalSyncService.collectDetailEnrichTargetContentIds(changedContentIds))
            .willReturn(changedContentIds)

        mockMvc.perform(
            post("/api/admin/festivals/sync-and-enrich")
                .param("pageNo", "1")
                .param("numOfRows", "200")
                .param("eventStartDate", "20260101")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("축제 목록 동기화가 완료되었고, 변경 또는 재처리 대상 축제의 상세 보강이 후속 처리됩니다."))
            .andExpect(jsonPath("$.data.totalCount").value(2))
            .andExpect(jsonPath("$.data.createdCount").value(1))
            .andExpect(jsonPath("$.data.updatedCount").value(1))
            .andExpect(jsonPath("$.data.failedCount").value(0))

        verify(festivalSyncService, times(1)).syncFestivalList(1, 200, "20260101")
        verify(festivalSyncService, times(1)).collectDetailEnrichTargetContentIds(changedContentIds)
        verify(festivalSyncService, times(1)).publishSyncCompletedEvent(changedContentIds, listResult)
    }

    @Test
    @DisplayName("sync-and-enrich 호출 시 목록 동기화가 실패하면 상세 보강을 수행하지 않고 Slack 알림만 전송한다")
    fun syncAndEnrichFestivals_list_failure_test() {
        val listResult = FestivalSyncResultResponse(
            totalCount = 0,
            createdCount = 0,
            updatedCount = 0,
            failedCount = 1,
            changedContentIds = emptyList()
        )

        given(festivalSyncService.syncFestivalList(1, 200, "20260101"))
            .willReturn(listResult)

        mockMvc.perform(
            post("/api/admin/festivals/sync-and-enrich")
                .param("pageNo", "1")
                .param("numOfRows", "200")
                .param("eventStartDate", "20260101")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("축제 목록 동기화에 실패하여 상세 보강은 수행하지 않았습니다."))
            .andExpect(jsonPath("$.data.totalCount").value(0))
            .andExpect(jsonPath("$.data.failedCount").value(1))

        verify(festivalSyncService, times(1)).syncFestivalList(1, 200, "20260101")
        verify(festivalSyncService, times(1)).notifyFestivalSyncResultOnly(listResult)
        verifyNoMoreInteractions(festivalSyncService)
    }
}
