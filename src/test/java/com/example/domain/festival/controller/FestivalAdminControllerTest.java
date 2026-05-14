package com.example.domain.festival.controller;

import com.example.domain.festival.dto.response.FestivalSyncStatusResponseDto;
import com.example.domain.festival.service.FestivalDetailSyncPendingService;
import com.example.domain.festival.service.FestivalSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}