package com.example.domain.festival.service;

import com.example.domain.festival.client.FestivalApiClient;
import com.example.domain.festival.converter.FestivalApiConverter;
import com.example.domain.festival.dto.external.*;
import com.example.domain.festival.dto.response.FestivalSyncResult;
import com.example.domain.festival.entity.DetailSyncPendingReason;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.event.FestivalSyncEventPublisher;
import com.example.domain.festival.repository.FestivalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class FestivalSyncServiceTest {

    private final FestivalApiClient festivalApiClient = mock(FestivalApiClient.class);
    private final FestivalApiConverter festivalApiConverter = mock(FestivalApiConverter.class);
    private final FestivalRepository festivalRepository = mock(FestivalRepository.class);
    private final FestivalSyncEventPublisher festivalSyncEventPublisher = mock(FestivalSyncEventPublisher.class);
    private final FestivalDetailSyncPendingService pendingService = mock(FestivalDetailSyncPendingService.class);

    private final FestivalSyncService festivalSyncService =
            new FestivalSyncService(
                    festivalApiClient,
                    festivalApiConverter,
                    festivalRepository,
                    festivalSyncEventPublisher,
                    pendingService
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
    @DisplayName("목록 동기화 예외 처리 테스트")
    class SyncFestivalListFailureTest {

        @Test
        @DisplayName("목록 동기화 중 한 건이 실패해도 failedCount에 반영되고 나머지는 계속 처리된다")
        void syncFestivalList_item_failure_test() throws Exception {
            FestivalApiItem item1 = createApiItem("1001", "가야문화축제");
            FestivalApiItem item2 = createApiItem("1002", "오류축제");

            FestivalApiResponse response = createResponse(List.of(item1, item2));

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
            when(festivalRepository.findAllByContentIdIn(List.of("1001", "1002"))).thenReturn(List.of());
            when(festivalApiConverter.toEntityFromListItem(item1)).thenReturn(newFestival);
            when(festivalApiConverter.toEntityFromListItem(item2))
                    .thenThrow(new RuntimeException("변환 실패"));

            FestivalSyncResult result = festivalSyncService.syncFestivalList(1, 10, "20260101");

            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getCreatedCount()).isEqualTo(1);
            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getChangedContentIds()).containsExactly("1001");

            verify(festivalRepository).save(newFestival);
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    class PublishSyncCompletedEventTest {

        @Test
        @DisplayName("변경된 contentId가 있으면 동기화 완료 이벤트를 발행한다")
        void publishSyncCompletedEvent_success_test() {
            List<String> changedContentIds = List.of("1001", "1002");

            festivalSyncService.publishSyncCompletedEvent(changedContentIds);

            verify(festivalSyncEventPublisher).publishSyncCompleted(changedContentIds);
        }

        @Test
        @DisplayName("변경된 contentId가 없으면 동기화 완료 이벤트를 발행하지 않는다")
        void publishSyncCompletedEvent_empty_test() {
            festivalSyncService.publishSyncCompletedEvent(List.of());

            verify(festivalSyncEventPublisher, never()).publishSyncCompleted(anyList());
        }
    }

    @Nested
    @DisplayName("상세 보강 대상 수집 테스트")
    class CollectDetailEnrichTargetContentIdsTest {

        @Test
        @DisplayName("목록 변경 대상과 pending 대상을 중복 없이 합쳐 반환한다")
        void collectDetailEnrichTargetContentIds_merge_changed_and_pending_test() {
            when(pendingService.findAllContentIds()).thenReturn(List.of("1002", "1003"));

            List<String> result =
                    festivalSyncService.collectDetailEnrichTargetContentIds(List.of("1001", "1002"));

            assertThat(result).containsExactly("1001", "1002", "1003");
            verify(pendingService).findAllContentIds();
        }
    }

    @Nested
    @DisplayName("상세 보강 테스트")
    class EnrichFestivalDetailsByContentIdsTest {

        @Test
        @DisplayName("상세 API 성공 응답이고 상세 정보가 변경된 경우 보강 후 pending 제거")
        void enrichFestivalDetailsByContentIds_success_test() throws Exception {
            Festival festival = Festival.builder()
                    .contentId("694576")
                    .title("가야문화축제")
                    .overview("기존 상세 설명")
                    .contactNumber(null)
                    .firstImageUrl("image1.jpg")
                    .thumbnailUrl("image2.jpg")
                    .address("경상남도 김해시 대성동")
                    .homepageUrl("https://old.com")
                    .startDate(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .endDate(LocalDateTime.of(2026, 5, 3, 23, 59, 59))
                    .mapX(128.87)
                    .mapY(35.23)
                    .lDongRegnCd("48")
                    .status(FestivalStatus.UPCOMING)
                    .build();

            FestivalApiItem detailItem = createApiItem("694576", "가야문화축제");
            setField(detailItem, "overview", "상세 설명");
            setField(detailItem, "homepage", "https://gcfkorea.com/");

            FestivalApiResponse detailResponse = createResponse(List.of(detailItem));

            when(festivalRepository.findByContentId("694576")).thenReturn(Optional.of(festival));
            when(festivalApiClient.fetchFestivalDetail("694576")).thenReturn(detailResponse);
            when(festivalApiConverter.isDetailIncomplete(festival)).thenReturn(false);
            when(festivalApiConverter.hasDetailChanges(festival, detailItem)).thenReturn(true);

            FestivalSyncResult result =
                    festivalSyncService.enrichFestivalDetailsByContentIds(List.of("694576"));

            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getCreatedCount()).isEqualTo(0);
            assertThat(result.getUpdatedCount()).isEqualTo(1);
            assertThat(result.getFailedCount()).isEqualTo(0);

            verify(festivalApiConverter).updateDetailFields(festival, detailItem);
            verify(pendingService).remove("694576");
        }

        @Test
        @DisplayName("상세 API 성공이지만 변경이 없더라도 pending 제거")
        void enrichFestivalDetailsByContentIds_success_no_change_remove_pending_test() throws Exception {
            Festival festival = Festival.builder()
                    .contentId("1001")
                    .title("축제1")
                    .overview("기존 상세 설명")
                    .address("서울")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(1))
                    .mapX(127.0)
                    .mapY(37.0)
                    .status(FestivalStatus.UPCOMING)
                    .build();

            FestivalApiItem detailItem = createApiItem("1001", "축제1");
            FestivalApiResponse detailResponse = createResponse(List.of(detailItem));

            when(festivalRepository.findByContentId("1001")).thenReturn(Optional.of(festival));
            when(festivalApiClient.fetchFestivalDetail("1001")).thenReturn(detailResponse);
            when(festivalApiConverter.isDetailIncomplete(festival)).thenReturn(false);
            when(festivalApiConverter.hasDetailChanges(festival, detailItem)).thenReturn(false);

            FestivalSyncResult result =
                    festivalSyncService.enrichFestivalDetailsByContentIds(List.of("1001"));

            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(0);

            verify(festivalApiConverter, never()).updateDetailFields(any(), any());
            verify(pendingService).remove("1001");
        }
    }

    @Nested
    @DisplayName("상세 보강 예외 정책 테스트")
    class EnrichFestivalDetailsFailureTest {

        @Test
        @DisplayName("상세 보강 중 429가 발생하면 현재 건과 뒤 미시도 건을 pending에 저장하고 즉시 중단한다")
        void enrichFestivalDetailsByContentIds_429_break_test() {
            Festival festival1 = Festival.builder()
                    .contentId("1001")
                    .title("축제1")
                    .overview("기존 상세 설명")
                    .address("서울")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(1))
                    .mapX(127.0)
                    .mapY(37.0)
                    .status(FestivalStatus.UPCOMING)
                    .build();

            Festival festival2 = Festival.builder()
                    .contentId("1002")
                    .title("축제2")
                    .overview("기존 상세 설명")
                    .address("부산")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(1))
                    .mapX(128.0)
                    .mapY(35.0)
                    .status(FestivalStatus.UPCOMING)
                    .build();

            when(festivalRepository.findByContentId("1001")).thenReturn(Optional.of(festival1));
            when(festivalRepository.findByContentId("1002")).thenReturn(Optional.of(festival2));
            when(festivalApiConverter.isDetailIncomplete(any(Festival.class))).thenReturn(false);

            when(festivalApiClient.fetchFestivalDetail("1001"))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Too Many Requests",
                            HttpHeaders.EMPTY,
                            new byte[0],
                            null
                    ));

            FestivalSyncResult result =
                    festivalSyncService.enrichFestivalDetailsByContentIds(List.of("1001", "1002"));

            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(1);

            verify(festivalApiClient).fetchFestivalDetail("1001");
            verify(festivalApiClient, never()).fetchFestivalDetail("1002");
            verify(pendingService).saveOrUpdate("1001", DetailSyncPendingReason.RATE_LIMIT);
            verify(pendingService).saveOrUpdate("1002", DetailSyncPendingReason.UNPROCESSED);
        }

        @Test
        @DisplayName("상세 보강 중 5xx가 발생하면 해당 건을 pending에 저장하고 다음 건으로 진행한다")
        void enrichFestivalDetailsByContentIds_5xx_continue_test() throws Exception {
            Festival festival1 = Festival.builder()
                    .contentId("1001")
                    .title("축제1")
                    .overview("기존 상세 설명")
                    .address("서울")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(1))
                    .mapX(127.0)
                    .mapY(37.0)
                    .status(FestivalStatus.UPCOMING)
                    .build();

            Festival festival2 = Festival.builder()
                    .contentId("1002")
                    .title("축제2")
                    .overview("기존 상세 설명")
                    .address("부산")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(1))
                    .mapX(128.0)
                    .mapY(35.0)
                    .status(FestivalStatus.UPCOMING)
                    .build();

            FestivalApiItem detailItem = createApiItem("1002", "축제2");
            setField(detailItem, "overview", "새 상세 설명");
            setField(detailItem, "homepage", "https://test.com");

            FestivalApiResponse detailResponse = createResponse(List.of(detailItem));

            when(festivalRepository.findByContentId("1001")).thenReturn(Optional.of(festival1));
            when(festivalRepository.findByContentId("1002")).thenReturn(Optional.of(festival2));
            when(festivalApiConverter.isDetailIncomplete(any(Festival.class))).thenReturn(false);

            when(festivalApiClient.fetchFestivalDetail("1001"))
                    .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
            when(festivalApiClient.fetchFestivalDetail("1002"))
                    .thenReturn(detailResponse);

            when(festivalApiConverter.hasDetailChanges(festival2, detailItem)).thenReturn(true);

            FestivalSyncResult result =
                    festivalSyncService.enrichFestivalDetailsByContentIds(List.of("1001", "1002"));

            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getUpdatedCount()).isEqualTo(1);
            assertThat(result.getFailedCount()).isEqualTo(1);

            verify(pendingService).saveOrUpdate("1001", DetailSyncPendingReason.SERVER_ERROR);
            verify(festivalApiConverter).updateDetailFields(festival2, detailItem);
            verify(pendingService).remove("1002");
        }

        @Test
        @DisplayName("상세 응답 구조가 비정상이면 pending에 저장한다")
        void enrichFestivalDetailsByContentIds_invalid_response_save_pending_test() {
            Festival festival = Festival.builder()
                    .contentId("1001")
                    .title("축제1")
                    .overview("기존 상세 설명")
                    .address("서울")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(1))
                    .mapX(127.0)
                    .mapY(37.0)
                    .status(FestivalStatus.UPCOMING)
                    .build();

            when(festivalRepository.findByContentId("1001")).thenReturn(Optional.of(festival));
            when(festivalApiClient.fetchFestivalDetail("1001")).thenReturn(null);
            when(festivalApiConverter.isDetailIncomplete(festival)).thenReturn(false);

            FestivalSyncResult result =
                    festivalSyncService.enrichFestivalDetailsByContentIds(List.of("1001"));

            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(1);

            verify(pendingService).saveOrUpdate("1001", DetailSyncPendingReason.EXCEPTION);
            verify(pendingService, never()).remove("1001");
        }
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