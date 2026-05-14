
package com.example.domain.festival.service;

import com.example.domain.festival.client.FestivalApiClient;
import com.example.domain.festival.converter.FestivalApiConverter;
import com.example.domain.festival.dto.external.FestivalApiItem;
import com.example.domain.festival.dto.external.FestivalApiResponse;
import com.example.domain.festival.dto.response.FestivalSyncResult;
import com.example.domain.festival.entity.DetailSyncPendingReason;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.event.FestivalSyncEventPublisher;
import com.example.domain.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FestivalSyncService {

    private final FestivalApiClient festivalApiClient;
    private final FestivalApiConverter festivalApiConverter;
    private final FestivalRepository festivalRepository;
    private final FestivalSyncEventPublisher festivalSyncEventPublisher;
    private final FestivalDetailSyncPendingService pendingService;

    /// 스케줄러 전용 실행 메서드 (로그 도입 X => 추후 확장 예정)
    @Transactional
    public void runScheduledSync(String eventStartDate, int pageNo, int numOfRows) {
        System.out.println("[FestivalScheduler] 축제 동기화 시작");
        System.out.println("pageNo=" + pageNo + ", numOfRows=" + numOfRows + ", eventStartDate=" + eventStartDate);

        try {
            FestivalSyncResult syncResult = syncFestivalList(pageNo, numOfRows, eventStartDate);

            System.out.println("[FestivalScheduler] 목록 동기화 완료");
            System.out.println("생성 건수: " + syncResult.getCreatedCount());
            System.out.println("수정 건수: " + syncResult.getUpdatedCount());
            System.out.println("실패 건수: " + syncResult.getFailedCount());

            List<String> detailTargetContentIds =
                    collectDetailEnrichTargetContentIds(syncResult.getChangedContentIds());

            System.out.println("[FestivalScheduler] 상세 보강 대상 건수: " + detailTargetContentIds.size());

            enrichFestivalDetailsByContentIds(detailTargetContentIds);

            System.out.println("[FestivalScheduler] 상세 보강 완료");

        } catch (Exception e) {
            System.out.println("[FestivalScheduler] 축제 동기화 실패");
            e.printStackTrace();
        }

        System.out.println("[FestivalScheduler] 축제 동기화 종료");
    }

    // 목록 API 기반 기본 축제 데이터 저장/수정
    public FestivalSyncResult syncFestivalList(int pageNo, int numOfRows, String eventStartDate) {

        //성능 TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long totalStart = System.currentTimeMillis();
        long apiStart = System.currentTimeMillis();

        FestivalApiResponse response =
                festivalApiClient.fetchFestivalList(pageNo, numOfRows, eventStartDate);

        //성능 TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long apiEnd = System.currentTimeMillis();

        // 빈 페이지는 예외가 아니라 0건 동기화 결과로 반환(0, ,0, 0, 0)
        if (response == null ||
                response.getResponse() == null ||
                response.getResponse().getBody() == null ||
                response.getResponse().getBody().getItems() == null ||
                response.getResponse().getBody().getItems().getItem() == null ||
                response.getResponse().getBody().getItems().getItem().isEmpty()) {
            return new FestivalSyncResult(0, 0, 0, 0, List.of());
        }

        List<FestivalApiItem> items = response.getResponse()
                .getBody()
                .getItems()
                .getItem();

        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;
        List<String> changedContentIds = new ArrayList<>();

        //성능TEST코드: DB 처리 시간 시간 (추후 삭제 가능)
        long dbStart = System.currentTimeMillis();

        // 목록 API 응답에서 contentId만 먼저 추출한다. (DB를 건별 조회X, 필요한 축제만 한 번에 조회하기 위함)
        List<String> contentIds = items.stream()
                .map(FestivalApiItem::getContentid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // contentId 목록으로 기존 축제를 한 번에 조회한다.
        List<Festival> existingFestivals = festivalRepository.findAllByContentIdIn(contentIds);

        // 조회한 축제를 contentId 기준 Map으로 변환한다. (item 순회 시 O(1)에 가깝게 기존 축제를 찾기 위함)
        Map<String, Festival> existingFestivalMap = existingFestivals.stream()
                .collect(Collectors.toMap(
                        Festival::getContentId,
                        Function.identity()
                ));

        //비교 저장 로직
        for (FestivalApiItem item : items) {
            try {
                String contentId = item.getContentid();

                // 미리 조회한 Map에서 기존 데이터를 꺼내서 사용
                Festival existingFestival = existingFestivalMap.get(contentId);

                // DB에 없는 신규 축제 → insert
                if (existingFestival == null) {
                    Festival newFestival = festivalApiConverter.toEntityFromListItem(item);
                    festivalRepository.save(newFestival);
                    createdCount++;
                    changedContentIds.add(contentId);
                }

                // DB에 존재하고 목록 필드가 변경된 경우 → update
                else if (festivalApiConverter.hasListChanges(existingFestival, item)) {
                    festivalApiConverter.updateFromListItem(existingFestival, item);
                    updatedCount++;
                    changedContentIds.add(contentId);
                }
            } catch (Exception e) {
                // item 단위 실패 처리 (전체 중단 방지)
                failedCount++;
                System.out.println("목록 동기화 실패 contentId=" + item.getContentid()
                        + ", message=" + e.getMessage());
            }
        }

        //성능TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long dbEnd = System.currentTimeMillis();
        long totalEnd = System.currentTimeMillis();

        // 목록 동기화 로그 출력
        System.out.println("[FestivalSync - List]");
        System.out.println("목록 조회 건수: " + items.size());
        System.out.println("목록 생성 건수: " + createdCount);
        System.out.println("목록 수정 건수: " + updatedCount);
        System.out.println("목록 실패 건수: " + failedCount);
        System.out.println("목록 변경 건수: " + changedContentIds.size());

        System.out.println("목록 API 소요시간: " + (apiEnd - apiStart) + "ms");
        System.out.println("목록 DB 소요시간: " + (dbEnd - dbStart) + "ms");
        System.out.println("목록 총 소요시간: " + (totalEnd - totalStart) + "ms");

        return new FestivalSyncResult(items.size(), createdCount, updatedCount, failedCount, changedContentIds);
    }


    //목록 동기화 완료 후, 변경된 contentId 목록에 대한 상세 보강 이벤트를 발행함
    public void publishSyncCompletedEvent(List<String> changedContentIds) {
        if (changedContentIds == null || changedContentIds.isEmpty()) {
            return;
        }

        festivalSyncEventPublisher.publishSyncCompleted(changedContentIds);
    }

    //상세 보강 대상 contentId 수집 (이번 목록 동기화에서 변경된 축제 + 기존 pending 대상)
    @Transactional(readOnly = true)
    public List<String> collectDetailEnrichTargetContentIds(List<String> changedContentIds) {
        Set<String> targetContentIds = new LinkedHashSet<>(changedContentIds);

        //성능TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long start = System.currentTimeMillis();

        // 이전 실행에서 실패/미시도된 상세 보강 대상도 함께 재처리
        targetContentIds.addAll(pendingService.findAllContentIds());

        //성능TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long end = System.currentTimeMillis();
        System.out.println("pending 조회 시간: " + (end - start) + "ms");

        // 상세 보강 대상 조회 로그
        System.out.println("상세 보강 대상 수: " + targetContentIds.size());
        System.out.println("상세 보강 대상 contentIds: " + targetContentIds);

        return new ArrayList<>(targetContentIds);
    }


    //상세 API 기반 상세 정보 보강 (변경된 contentId 목록만 변경 대상<ex. 초기적재 or 실제 변경> + 이전 실행에서 실패/미시도된 pending 축제)
    //
    public FestivalSyncResult enrichFestivalDetailsByContentIds(List<String> contentIds) {
        int updatedCount = 0;
        int failedCount = 0;

        long beforePendingCount = pendingService.count();   // 실행 전 pending 건수
        long beforePendingFailureCount =
                pendingService.countByReason(DetailSyncPendingReason.RATE_LIMIT)
                        + pendingService.countByReason(DetailSyncPendingReason.SERVER_ERROR)
                        + pendingService.countByReason(DetailSyncPendingReason.EXCEPTION);
        long beforePendingUnprocessedCount =
                pendingService.countByReason(DetailSyncPendingReason.UNPROCESSED);


        int newPendingCount = 0;                            // 이번 실행에서 새로 pending 처리된 건수

        //성능TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long totalStart = System.currentTimeMillis();
        //성능TEST코드: 상세 API 호출 횟수 (추후 삭제 가능)
        int apiCallCount = 0;
        //로그 관리용 변수: 중단 사유
        String stopReason = null;

        for (int i = 0; i < contentIds.size(); i++) {
            String contentId = contentIds.get(i);

            try {
                Festival festival = festivalRepository.findByContentId(contentId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "해당 contentId의 축제를 찾을 수 없습니다. contentId=" + contentId));

                boolean wasDetailIncomplete = festivalApiConverter.isDetailIncomplete(festival);

                //성능TEST코드: API 시간 호출 시간 및 호출 횟수 (추후 삭제 가능)
                long apiStart = System.currentTimeMillis();
                apiCallCount++;

                FestivalApiResponse detailResponse =
                        festivalApiClient.fetchFestivalDetail(contentId);

                //성능TEST코드: API 시간 호출 시간 (추후 삭제 가능)
                long apiEnd = System.currentTimeMillis();
                System.out.println("상세 API 1건: " + (apiEnd - apiStart) + "ms");

                // 응답 구조 이상 또는 resultCode 비정상 → 실패 처리 + pending 저장
                if (detailResponse == null ||
                        detailResponse.getResponse() == null ||
                        detailResponse.getResponse().getHeader() == null ||
                        !"0000".equals(detailResponse.getResponse().getHeader().getResultCode())) {
                    failedCount++;
                    pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
                    newPendingCount++;
                    continue;
                }

                // body / items 구조 이상 → 실패 처리 + pending 저장
                if (detailResponse.getResponse().getBody() == null ||
                        detailResponse.getResponse().getBody().getItems() == null ||
                        detailResponse.getResponse().getBody().getItems().getItem() == null) {
                    failedCount++;
                    pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
                    newPendingCount++;
                    continue;
                }

                List<FestivalApiItem> detailItems = detailResponse.getResponse()
                        .getBody()
                        .getItems()
                        .getItem();

                if (detailItems.isEmpty()) {
                    failedCount++;
                    pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
                    newPendingCount++;
                    continue;
                }

                FestivalApiItem detailItem = detailItems.get(0);

                //상세 정보도 실제 변경된 경우에만 update 수행
                if (wasDetailIncomplete || festivalApiConverter.hasDetailChanges(festival, detailItem)) {
                    festivalApiConverter.updateDetailFields(festival, detailItem);

                    // 기존에 상세가 있었던 축제가 실제 변경된 경우만 updatedCount 증가
                    if (!wasDetailIncomplete) {
                        updatedCount++;
                    }
                }

                // 상세 API 호출 및 응답 검증이 정상 완료되었으므로, 이전 실패 이력이 있어도 pending에서 제거(재처리 대상 X)
                pendingService.remove(contentId);

            } catch (HttpClientErrorException.TooManyRequests e) {
                // 429 발생 시 quota 보호를 위해 전체 상세 보강 즉시 중단
                // 단, 현재 실패 대상과 뒤의 미시도 대상은 pending에 저장하여 다음 실행 때 재처리할 수 있도록 한다.
                failedCount++;
                stopReason = "429 (quota 초과)";

                // 현재 실패한 대상 저장
                pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.RATE_LIMIT);
                newPendingCount++;

                // 아직 시도하지 못한 뒤의 대상들은 미시도 상태로 pending 저장
                for (int j = i + 1; j < contentIds.size(); j++) {
                    pendingService.saveOrUpdate(contentIds.get(j), DetailSyncPendingReason.UNPROCESSED);
                    newPendingCount++;
                }

                System.out.println("429 발생 → 상세 보강 전체 중단 contentId=" + contentId);
                break;

            } catch (HttpServerErrorException e) {
                // 5xx 오류 → 해당 대상만 실패 처리 후 계속 진행
                failedCount++;
                pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.SERVER_ERROR);
                newPendingCount++;

                if (stopReason == null) {
                    stopReason = "5xx 서버 오류 (" + e.getStatusCode() + ")";
                }

                System.out.println("외부 API 서버 오류 → 상세 보강 실패 contentId=" + contentId
                        + ", status=" + e.getStatusCode());

            } catch (Exception e) {
                // 기타 예외 → 해당 대상만 실패 처리
                failedCount++;
                pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
                newPendingCount++;

                System.out.println("상세 보강 실패 contentId=" + contentId
                        + ", message=" + e.getMessage());
            }
        }

        //성능TEST코드: API 시간 호출 시간 (추후 삭제 가능)
        long totalEnd = System.currentTimeMillis();
        long afterPendingCount = pendingService.count();    // 실행 후 남은 pending 건수

        //로그 변수
        int totalTargetCount = contentIds.size();   // 전체 대상
        int attemptedCount = apiCallCount;  // 실제 호출
        int skippedCount = totalTargetCount - attemptedCount;   // 미시도
        int successCount = updatedCount;    // 성공
        int failureCount = failedCount; // 실패 (시도했지만 실패한 건)
        int unprocessedCount = failureCount + skippedCount; // 미처리 (실패 + 미시도)
        String finalStopReason = (stopReason == null) ? "없음 (정상 처리 또는 일부 실패)" : stopReason; // 중단 사유 (없으면 정상 종료)

        // 상세 정보 동기화 로그 출력
        System.out.println("[FestivalSync - Detail]");
        System.out.println("기존 pending 대상 건수: " + beforePendingCount
                + " (실패 " + beforePendingFailureCount
                + ", 미시도 " + beforePendingUnprocessedCount + ")");
        System.out.println("상세 보강 대상 건수: " + totalTargetCount);

        System.out.println("상세 보강 업데이트 건수: " + updatedCount);
        System.out.println("상세 보강 실패 건수: " + failureCount);
        System.out.println("상세 보강 미처리 건수: " + unprocessedCount
                + " (실패 " + failureCount + " + 미시도 " + skippedCount + ")");

        System.out.println("이번 실행 신규 pending 추가 건수: " + newPendingCount);
        System.out.println("실행 후 남은 pending 건수: " + afterPendingCount);

        System.out.println("상세 API 호출 시도 횟수: " + attemptedCount);
        System.out.println("상세 API 미시도 횟수: " + skippedCount);
        System.out.println("중단 사유: " + finalStopReason);
        System.out.println("상세 보강 총 소요시간: " + (totalEnd - totalStart) + "ms");

        return new FestivalSyncResult(contentIds.size(), 0, updatedCount, failedCount, contentIds);
        }



    //상세 API 기반 상세 정보 보강 (특정 축제 1건에 대해한 상세 정보를 보강한다)
    //특정 데이터에 문제가 발생했을 때 부분 재동기화 용도로 활용, API 호출을 1건만 수행하므로 rate limit(429) 부담이 적음
// 상세 API 기반 상세 정보 보강 (특정 축제 1건)
    @Transactional
    public void enrichFestivalDetailByContentId(String contentId) {
        Festival festival = festivalRepository.findByContentId(contentId)
                .orElseThrow(() -> new NoSuchElementException(
                        "해당 contentId의 축제를 찾을 수 없습니다. contentId=" + contentId));

        FestivalApiResponse response =
                festivalApiClient.fetchFestivalDetail(contentId);

        if (response == null ||
                response.getResponse() == null ||
                response.getResponse().getHeader() == null ||
                !"0000".equals(response.getResponse().getHeader().getResultCode())) {
            pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
            return;
        }

        if (response.getResponse().getBody() == null ||
                response.getResponse().getBody().getItems() == null ||
                response.getResponse().getBody().getItems().getItem() == null) {
            pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
            return;
        }

        List<FestivalApiItem> items = response.getResponse()
                .getBody()
                .getItems()
                .getItem();

        if (items.isEmpty()) {
            pendingService.saveOrUpdate(contentId, DetailSyncPendingReason.EXCEPTION);
            return;
        }

        FestivalApiItem detailItem = items.get(0);

        if (festivalApiConverter.hasDetailChanges(festival, detailItem)) {
            festivalApiConverter.updateDetailFields(festival, detailItem);
        }

        // 1건 재동기화가 정상 종료되었으므로 pending 제거
        pendingService.remove(contentId);
    }
    @Transactional
    public void updateFestivalStatuses() {
        LocalDateTime now = LocalDateTime.now();
        // 1. 종료일이 지난 축제를 ENDED로 변경
        int endedCount = festivalRepository.updateStatusToEnded(FestivalStatus.ENDED, now);

        // 2. 시작일이 오늘이거나 어제인데 아직 UPCOMING인 축제를 ONGOING으로 변경
        int ongoingCount = festivalRepository.updateStatusToOngoing(FestivalStatus.ONGOING, FestivalStatus.UPCOMING, now);

        System.out.println("[FestivalStatus] 상태 업데이트 완료: 진행중 전환 " + ongoingCount + "건, 종료 전환 " + endedCount + "건");
    }
}

