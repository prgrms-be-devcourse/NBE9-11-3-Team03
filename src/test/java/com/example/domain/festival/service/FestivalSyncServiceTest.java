package com.example.domain.festival.service;

import com.example.domain.festival.client.FestivalApiClient;
import com.example.domain.festival.converter.FestivalApiConverter;
import com.example.domain.festival.dto.external.*;
import com.example.domain.festival.dto.response.FestivalSyncResult;
import com.example.domain.festival.dto.response.FestivalSyncStatusResponseDto;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.event.FestivalSyncEventPublisher;
import com.example.domain.festival.notification.FestivalSyncSlackMessageFactory;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.global.notification.SlackNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class FestivalSyncServiceTest {

    private final FestivalApiClient festivalApiClient = mock(FestivalApiClient.class);
    private final FestivalApiConverter festivalApiConverter = mock(FestivalApiConverter.class);
    private final FestivalRepository festivalRepository = mock(FestivalRepository.class);
    private final FestivalSyncEventPublisher festivalSyncEventPublisher = mock(FestivalSyncEventPublisher.class);
    private final FestivalDetailSyncPendingService pendingService = mock(FestivalDetailSyncPendingService.class);
    private final SlackNotificationService slackNotificationService = mock(SlackNotificationService.class);
    private final FestivalSyncSlackMessageFactory festivalSyncSlackMessageFactory = mock(FestivalSyncSlackMessageFactory.class);

    private final FestivalSyncService festivalSyncService = new FestivalSyncService(
            festivalApiClient,
            festivalApiConverter,
            festivalRepository,
            festivalSyncEventPublisher,
            pendingService,
            slackNotificationService,
            festivalSyncSlackMessageFactory
    );

    @Nested
    @DisplayName("목록 동기화 테스트")
    class SyncFestivalListTest {

        @Test
        @DisplayName("신규 축제면 저장한다")
        void syncFestivalList_create_test() throws Exception {
            FestivalApiItem item = createApiItem("1001", "가야문화축제");
            FestivalApiResponse response = createResponse(List.of(item));

            Festival newFestival = Festival.builder()
                    .contentId("1001")
                    .title("가야문화축제")
                    .overview("상세 설명 없음")
                    .contactNumber("055-330-6840")
                    .firstImageUrl("image1.jpg")
                    .thumbnailUrl("image2.jpg")
                    .address("경상남도 김해시 대성동")
                    .homepageUrl("https://test.com")
                    .startDate(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .endDate(LocalDateTime.of(2026, 5, 3, 23, 59, 59))
                    .mapX(128.87)
                    .mapY(35.23)
                    .lDongRegnCd("48")
                    .status(FestivalStatus.UPCOMING)
                    .build();

            when(festivalApiClient.fetchFestivalList(1, 10, "20260101")).thenReturn(response);
            when(festivalRepository.findAllByContentIdIn(List.of("1001"))).thenReturn(List.of());
            when(festivalApiConverter.toEntityFromListItem(item)).thenReturn(newFestival);

            FestivalSyncResult result = festivalSyncService.syncFestivalList(1, 10, "20260101");

            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getCreatedCount()).isEqualTo(1);
            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(0);
            assertThat(result.getChangedContentIds()).containsExactly("1001");

            verify(festivalRepository).save(newFestival);
            verify(festivalApiConverter).toEntityFromListItem(item);
        }

        @Test
        @DisplayName("기존 축제면서 목록 정보가 변경된 경우 수정한다")
        void syncFestivalList_update_test() throws Exception {
            FestivalApiItem item = createApiItem("1001", "수정된 축제명");
            FestivalApiResponse response = createResponse(List.of(item));

            Festival existingFestival = Festival.builder()
                    .contentId("1001")
                    .title("기존 축제명")
                    .overview("기존 설명")
                    .contactNumber("055-1111-1111")
                    .firstImageUrl("old1.jpg")
                    .thumbnailUrl("old2.jpg")
                    .address("기존 주소")
                    .homepageUrl("https://old.com")
                    .startDate(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .endDate(LocalDateTime.of(2026, 4, 2, 23, 59, 59))
                    .mapX(127.0)
                    .mapY(37.0)
                    .lDongRegnCd("11")
                    .status(FestivalStatus.UPCOMING)
                    .build();

            when(festivalApiClient.fetchFestivalList(1, 10, "20260101")).thenReturn(response);
            when(festivalRepository.findAllByContentIdIn(List.of("1001")))
                    .thenReturn(List.of(existingFestival));
            when(festivalApiConverter.hasListChanges(existingFestival, item)).thenReturn(true);

            FestivalSyncResult result = festivalSyncService.syncFestivalList(1, 10, "20260101");

            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getCreatedCount()).isEqualTo(0);
            assertThat(result.getUpdatedCount()).isEqualTo(1);
            assertThat(result.getFailedCount()).isEqualTo(0);
            assertThat(result.getChangedContentIds()).containsExactly("1001");

            verify(festivalApiConverter).updateFromListItem(existingFestival, item);
            verify(festivalRepository, never()).save(any(Festival.class));
        }

        @Test
        @DisplayName("기존 축제지만 목록 정보가 변경되지 않은 경우 수정하지 않는다")
        void syncFestivalList_no_change_test() throws Exception {
            FestivalApiItem item = createApiItem("1001", "기존 축제명");
            FestivalApiResponse response = createResponse(List.of(item));

            Festival existingFestival = Festival.builder()
                    .contentId("1001")
                    .title("기존 축제명")
                    .overview("기존 설명")
                    .contactNumber("055-1111-1111")
                    .firstImageUrl("old1.jpg")
                    .thumbnailUrl("old2.jpg")
                    .address("기존 주소")
                    .homepageUrl("https://old.com")
                    .startDate(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .endDate(LocalDateTime.of(2026, 4, 2, 23, 59, 59))
                    .mapX(127.0)
                    .mapY(37.0)
                    .lDongRegnCd("11")
                    .status(FestivalStatus.UPCOMING)
                    .build();

            when(festivalApiClient.fetchFestivalList(1, 10, "20260101")).thenReturn(response);
            when(festivalRepository.findAllByContentIdIn(List.of("1001")))
                    .thenReturn(List.of(existingFestival));
            when(festivalApiConverter.hasListChanges(existingFestival, item)).thenReturn(false);

            FestivalSyncResult result = festivalSyncService.syncFestivalList(1, 10, "20260101");

            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getCreatedCount()).isEqualTo(0);
            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(0);
            assertThat(result.getChangedContentIds()).isEmpty();

            verify(festivalApiConverter, never()).updateFromListItem(any(), any());
            verify(festivalRepository, never()).save(any(Festival.class));
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    class PublishSyncCompletedEventTest {

        @Test
        @DisplayName("변경된 contentId가 있으면 목록 결과와 함께 동기화 완료 이벤트를 발행한다")
        void publishSyncCompletedEvent_success_test() {
            // given
            List<String> changedContentIds = List.of("1001", "1002");

            FestivalSyncResult listResult =
                    new FestivalSyncResult(
                            2,
                            1,
                            1,
                            0,
                            changedContentIds
                    );

            // when
            festivalSyncService.publishSyncCompletedEvent(changedContentIds, listResult);

            // then
            verify(festivalSyncEventPublisher, times(1))
                    .publishSyncCompleted(changedContentIds, listResult);
        }

        @Test
        @DisplayName("변경된 contentId가 없으면 동기화 완료 이벤트를 발행하지 않는다")
        void publishSyncCompletedEvent_empty_test() {
            // given
            FestivalSyncResult listResult =
                    new FestivalSyncResult(
                            0,
                            0,
                            0,
                            0,
                            List.of()
                    );

            // when
            festivalSyncService.publishSyncCompletedEvent(List.of(), listResult);

            // then
            verify(festivalSyncEventPublisher, never())
                    .publishSyncCompleted(anyList(), any());
        }
    }

    @Test
    @DisplayName("상세 보강 대상이 없으면 목록 결과만으로 Slack 알림을 전송한다")
    void notifyFestivalSyncResultOnly_test() {
        // given
        FestivalSyncResult listResult =
                new FestivalSyncResult(200, 0, 0, 0, List.of());

        FestivalSyncStatusResponseDto status =
                new FestivalSyncStatusResponseDto(
                        0L,
                        Map.of(
                                "RATE_LIMIT", 0L,
                                "SERVER_ERROR", 0L,
                                "EXCEPTION", 0L,
                                "UNPROCESSED", 0L
                        ),
                        false
                );

        String message = "[축제 데이터 동기화 결과] 변경 없음";

        given(pendingService.getSyncStatus()).willReturn(status);

        given(festivalSyncSlackMessageFactory.createMessage(
                eq(listResult),
                any(FestivalSyncResult.class),
                eq(status)
        )).willReturn(message);

        // when
        festivalSyncService.notifyFestivalSyncResultOnly(listResult);

        // then
        verify(pendingService, times(1)).getSyncStatus();

        verify(festivalSyncSlackMessageFactory, times(1))
                .createMessage(
                        eq(listResult),
                        argThat(detailResult ->
                                detailResult.getTotalCount() == 0
                                        && detailResult.getCreatedCount() == 0
                                        && detailResult.getUpdatedCount() == 0
                                        && detailResult.getFailedCount() == 0
                                        && detailResult.getChangedContentIds().isEmpty()
                        ),
                        eq(status)
                );

        verify(slackNotificationService, times(1)).sendMessage(message);
    }

    @Test
    @DisplayName("상세 보강 완료 후 목록 결과와 상세 결과를 포함해 Slack 알림을 전송한다")
    void enrichFestivalDetailsAndNotify_test() {
        // given
        List<String> contentIds = List.of();

        FestivalSyncResult listResult =
                new FestivalSyncResult(200, 3, 2, 0, List.of("1001", "1002"));

        FestivalSyncStatusResponseDto status =
                new FestivalSyncStatusResponseDto(
                        0L,
                        Map.of(
                                "RATE_LIMIT", 0L,
                                "SERVER_ERROR", 0L,
                                "EXCEPTION", 0L,
                                "UNPROCESSED", 0L
                        ),
                        false
                );

        String message = "[축제 데이터 동기화 결과] 성공";

        given(pendingService.count()).willReturn(0L);
        given(pendingService.countByReason(any())).willReturn(0L);
        given(pendingService.getSyncStatus()).willReturn(status);

        given(festivalSyncSlackMessageFactory.createMessage(
                eq(listResult),
                any(FestivalSyncResult.class),
                eq(status)
        )).willReturn(message);

        // when
        festivalSyncService.enrichFestivalDetailsAndNotify(contentIds, listResult);

        // then
        verify(pendingService, times(1)).getSyncStatus();

        verify(festivalSyncSlackMessageFactory, times(1))
                .createMessage(
                        eq(listResult),
                        argThat(detailResult ->
                                detailResult.getTotalCount() == 0
                                        && detailResult.getCreatedCount() == 0
                                        && detailResult.getUpdatedCount() == 0
                                        && detailResult.getFailedCount() == 0
                        ),
                        eq(status)
                );

        verify(slackNotificationService, times(1)).sendMessage(message);
    }

    private FestivalApiItem createApiItem(String contentId, String title) throws Exception {
        FestivalApiItem item = new FestivalApiItem();

        setField(item, "contentid", contentId);
        setField(item, "title", title);
        setField(item, "overview", "상세 설명 없음");
        setField(item, "tel", "055-330-6840");
        setField(item, "addr1", "경상남도 김해시");
        setField(item, "addr2", "대성동");
        setField(item, "homepage", "https://test.com");
        setField(item, "firstimage", "image1.jpg");
        setField(item, "firstimage2", "image2.jpg");
        setField(item, "mapx", "128.87");
        setField(item, "mapy", "35.23");
        setField(item, "lDongRegnCd", "48");
        setField(item, "eventstartdate", "20260430");
        setField(item, "eventenddate", "20260503");

        return item;
    }

    private FestivalApiResponse createResponse(List<FestivalApiItem> itemList) throws Exception {
        FestivalApiHeader header = new FestivalApiHeader();

        setField(header, "resultCode", "0000");
        setField(header, "resultMsg", "OK");

        FestivalApiItems items = new FestivalApiItems();
        setField(items, "item", itemList);

        FestivalApiBody body = new FestivalApiBody();
        setField(body, "items", items);
        setField(body, "numOfRows", itemList.size());
        setField(body, "pageNo", 1);
        setField(body, "totalCount", itemList.size());

        FestivalApiResponse.Response responseInner = new FestivalApiResponse.Response();

        setField(responseInner, "header", header);
        setField(responseInner, "body", body);

        FestivalApiResponse response = new FestivalApiResponse();

        setField(response, "response", responseInner);

        return response;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}