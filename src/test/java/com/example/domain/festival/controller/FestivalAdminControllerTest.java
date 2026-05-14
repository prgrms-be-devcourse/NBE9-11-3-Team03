package com.example.domain.festival.controller;

import com.example.domain.festival.dto.response.FestivalSyncResult;
import com.example.domain.festival.dto.response.FestivalSyncStatusResponseDto;
import com.example.domain.festival.service.FestivalDetailSyncPendingService;
import com.example.domain.festival.service.FestivalSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class FestivalAdminControllerTest {

    private MockMvc mockMvc;

    private FestivalSyncService festivalSyncService;
    private FestivalDetailSyncPendingService pendingService;

    @BeforeEach
    void setUp() {
        festivalSyncService = mock(FestivalSyncService.class);
        pendingService = mock(FestivalDetailSyncPendingService.class);

        FestivalAdminController controller =
                new FestivalAdminController(festivalSyncService, pendingService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("sync-status 조회 시 재처리 대상이 없으면 정상 상태 메시지를 반환한다")
    void getFestivalSyncStatus_no_retry_test() throws Exception {
        FestivalSyncStatusResponseDto response = new FestivalSyncStatusResponseDto(
                0L,
                Map.of(
                        "RATE_LIMIT", 0L,
                        "SERVER_ERROR", 0L,
                        "EXCEPTION", 0L,
                        "UNPROCESSED", 0L
                ),
                false
        );

        given(pendingService.getSyncStatus()).willReturn(response);

        mockMvc.perform(get("/api/admin/festivals/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("축제 동기화가 정상 상태입니다. 재처리할 상세 대상이 없습니다."))
                .andExpect(jsonPath("$.data.pendingCount").value(0))
                .andExpect(jsonPath("$.data.needsRetry").value(false))
                .andExpect(jsonPath("$.data.pendingBreakdown.RATE_LIMIT").value(0))
                .andExpect(jsonPath("$.data.pendingBreakdown.UNPROCESSED").value(0));
    }

    @Test
    @DisplayName("sync-status 조회 시 재처리 대상이 있으면 재처리 필요 메시지를 반환한다")
    void getFestivalSyncStatus_need_retry_test() throws Exception {
        FestivalSyncStatusResponseDto response = new FestivalSyncStatusResponseDto(
                3L,
                Map.of(
                        "RATE_LIMIT", 1L,
                        "SERVER_ERROR", 0L,
                        "EXCEPTION", 0L,
                        "UNPROCESSED", 2L
                ),
                true
        );

        given(pendingService.getSyncStatus()).willReturn(response);

        mockMvc.perform(get("/api/admin/festivals/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("API 호출 제한으로 인해 상세 동기화 재처리가 필요합니다."))
                .andExpect(jsonPath("$.data.pendingCount").value(3))
                .andExpect(jsonPath("$.data.needsRetry").value(true))
                .andExpect(jsonPath("$.data.pendingBreakdown.RATE_LIMIT").value(1))
                .andExpect(jsonPath("$.data.pendingBreakdown.UNPROCESSED").value(2));
    }

    @Test
    @DisplayName("sync-and-enrich 호출 시 목록 동기화 결과를 반환하고 상세 보강 이벤트를 발행한다")
    void syncAndEnrichFestivals_success_test() throws Exception {
        // given
        FestivalSyncResult listResult = new FestivalSyncResult(
                2,
                1,
                1,
                0,
                List.of("1001", "1002")
        );

        given(festivalSyncService.syncFestivalList(1, 200, "20260101"))
                .willReturn(listResult);

        given(festivalSyncService.collectDetailEnrichTargetContentIds(List.of("1001", "1002")))
                .willReturn(List.of("1001", "1002"));

        // when & then
        mockMvc.perform(post("/api/admin/festivals/sync-and-enrich")
                        .param("pageNo", "1")
                        .param("numOfRows", "200")
                        .param("eventStartDate", "20260101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("축제 목록 동기화가 완료되었고, 변경 또는 재처리 대상 축제의 상세 보강이 후속 처리됩니다."))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.updatedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        verify(festivalSyncService, times(1))
                .syncFestivalList(1, 200, "20260101");

        verify(festivalSyncService, times(1))
                .collectDetailEnrichTargetContentIds(List.of("1001", "1002"));

        verify(festivalSyncService, times(1))
                .publishSyncCompletedEvent(List.of("1001", "1002"));
    }
}