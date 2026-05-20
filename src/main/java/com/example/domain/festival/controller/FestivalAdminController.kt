package com.example.domain.festival.controller

import com.example.domain.festival.dto.response.FestivalSyncResponse
import com.example.domain.festival.dto.response.FestivalSyncStatusResponse
import com.example.domain.festival.service.FestivalDetailSyncPendingService
import com.example.domain.festival.service.FestivalSyncService
import com.example.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/festivals")
@Tag(name = "Festival Admin", description = "축제 관리자 API")
class FestivalAdminController(
    private val festivalSyncService: FestivalSyncService,
    private val pendingService: FestivalDetailSyncPendingService
) {
    //메인관리자 동기화 API
    //1. 목록 동기화 수행 (신규/변경 데이터 반영)
    //2. 상세 보강 대상 수집(목록 동기화 변경 대상 + 기존 pending 대상) (실패/미시도)
    //3. 상세 보강 후속 처리 이벤트 발행
    // 본 응답에는 목록 동기화 결과만 포함되며, 상세 보강 완료 결과는 포함되지 않으며 후속처리로 수행된다.
    @PostMapping("/sync-and-enrich")
    @Operation(
        summary = "축제 목록 동기화 및 상세 정보 수집",
        description = "공공 API에서 축제 목록을 동기화한 후, 변경된 축제들의 상세 정보 보강을 비동기로 수행합니다. 응답은 목록 동기화 결과만 포함합니다."
    )
    fun syncAndEnrichFestivals(
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "200") numOfRows: Int,
        @RequestParam(defaultValue = "20260101") eventStartDate: String
    ): RsData<FestivalSyncResponse> {

        val listResult =
            festivalSyncService.syncFestivalList(pageNo, numOfRows, eventStartDate)

        if (listResult.totalCount == 0 && listResult.failedCount > 0) {
            festivalSyncService.notifyFestivalSyncResultOnly(listResult)

            val response = FestivalSyncResponse(
                listResult.totalCount,
                listResult.createdCount,
                listResult.updatedCount,
                listResult.failedCount
            )

            return RsData.success(
                "축제 목록 동기화에 실패하여 상세 보강은 수행하지 않았습니다.",
                response
            )
        }

        // 이번 목록 변경분 + 이전 상세 실패/미시도 대상까지 합쳐 상세 보강 대상으로 수집
        val targetContentIds =
            festivalSyncService.collectDetailEnrichTargetContentIds(listResult.changedContentIds)

        // 수집된 대상 기준으로 상세 보강 이벤트 발행
        if (targetContentIds.isEmpty()) {
            festivalSyncService.notifyFestivalSyncResultOnly(listResult)
        } else {
            festivalSyncService.publishSyncCompletedEvent(targetContentIds, listResult)
        }

        val response = FestivalSyncResponse(
            listResult.totalCount,
            listResult.createdCount,
            listResult.updatedCount,
            listResult.failedCount
        )

        val hasFailedItems = listResult.failedCount > 0
        val hasDetailTargets = targetContentIds.isNotEmpty()

        val message =
            if (!hasFailedItems && !hasDetailTargets) {
                "축제 목록 동기화가 완료되었고, 상세 보강 대상은 없습니다."
            } else if (!hasFailedItems) {
                "축제 목록 동기화가 완료되었고, 변경 또는 재처리 대상 축제의 상세 보강이 후속 처리됩니다."
            } else if (!hasDetailTargets) {
                "축제 목록 동기화가 부분 완료되었습니다. 일부 축제 목록은 처리되지 않았으며, 상세 보강 대상은 없습니다."
            } else {
                "축제 목록 동기화가 부분 완료되었습니다. 일부 축제 목록은 처리되지 않았으며, 변경 또는 재처리 대상 축제의 상세 보강이 후속 처리됩니다."
            }

        return RsData.success(message, response)
    }

    //축제 목록 데이터를 수동 동기화한다. (공공 API 목록 조회 -> contentId 기준으로 insert / 변경사항 확인 후 ,update 수행)
    @PostMapping("/sync-list")
    @Operation(
        summary = "축제 목록만 동기화",
        description = "공공 API에서 축제 목록만 동기화합니다. 상세 정보는 동기화하지 않습니다. 디버깅 및 상세 보강 문제 확인용입니다."
    )
    fun syncFestivals(
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "10") numOfRows: Int,
        @RequestParam(defaultValue = "20260101") eventStartDate: String
    ): RsData<FestivalSyncResponse> {
        val result =
            festivalSyncService.syncFestivalList(pageNo, numOfRows, eventStartDate)

        val response = FestivalSyncResponse(
            result.totalCount,
            result.createdCount,
            result.updatedCount,
            result.failedCount
        )

        return RsData.success("축제 목록 동기화가 완료되었습니다.", response)
    }

    //이전 실행에서 실패하거나 미시도된 pending 대상 축제를 기준으로 상세 보강 재처리를 수행한다. (축제 상세 정보 재동기화 목적)
    @PostMapping("/enrich-pending")
    @Operation(summary = "미처리 축제 상세 보강 재처리", description = "이전 동기화에서 실패하거나 미처리된 축제들의 상세 정보를 다시 보강합니다.")
    fun enrichAllFestivalDetails(): RsData<FestivalSyncResponse> {
        val targetContentIds =
            festivalSyncService.collectDetailEnrichTargetContentIds(emptyList())

        if (targetContentIds.isEmpty()) {
            return RsData.success(
                "상세 보강 재처리 대상 축제가 없습니다.",
                FestivalSyncResponse(0, 0, 0, 0)
            )
        }

        val result =
            festivalSyncService.enrichFestivalDetailsByContentIds(targetContentIds)

        val response = FestivalSyncResponse(
            result.totalCount,
            result.createdCount,
            result.updatedCount,
            result.failedCount
        )

        val message =
            if (result.updatedCount == 0 && result.failedCount > 0) {
                "축제 상세 보강 재처리가 실패했습니다. 외부 API 제한 또는 오류로 인해 처리되지 않았습니다."
            } else if (result.failedCount > 0) {
                "축제 상세 보강 재처리가 부분 완료되었습니다. 일부 대상은 외부 API 제한 또는 오류로 처리되지 않았습니다."
            } else {
                "축제 상세 보강 재처리가 완료되었습니다."
            }

        return RsData.success(message, response)
    }

    //특정 축제 1건만 상세 보강한다.(특정 데이터 재동기화 , 디버깅, 전체 보강 전 검증 목적)
    @PostMapping("/{contentId}/enrich")
    @Operation(summary = "특정 축제 상세 보강", description = "contentId로 지정된 단일 축제의 상세 정보를 보강합니다. 디버깅 및 검증 목적입니다.")
    fun enrichFestivalByContentId(@PathVariable contentId: String): RsData<Void> {
        festivalSyncService.enrichFestivalDetailByContentId(contentId)
        return RsData.success("특정 축제 상세 정보 보강이 완료되었습니다.")
    }

    @GetMapping("/sync-status")
    @Operation(
        summary = "축제 동기화 상태 조회",
        description = "현재 진행 중인 축제 상세 동기화의 상태를 조회합니다. 실패 및 미처리 대상 개수를 포함합니다."
    )
    fun getFestivalSyncStatus(): RsData<FestivalSyncStatusResponse> {
        val response = pendingService.getSyncStatus()
        val breakdown = response.pendingBreakdown

        val message =
            if (!response.needsRetry) {
                "축제 동기화가 정상 상태입니다. 재처리할 상세 대상이 없습니다."
            } else if (breakdown.getOrDefault("RATE_LIMIT", 0L) > 0) {
                "API 호출 제한으로 인해 상세 동기화 재처리가 필요합니다."
            } else if (
                breakdown.getOrDefault("SERVER_ERROR", 0L) > 0 ||
                breakdown.getOrDefault("EXCEPTION", 0L) > 0
            ) {
                "상세 동기화 실패 대상이 존재합니다. 재처리가 필요합니다."
            } else {
                "미처리된 상세 동기화 대상이 존재합니다. 재처리가 필요합니다."
            }

        return RsData.success(message, response)
    }

    @PostMapping("/update-status")
    @Operation(
        summary = "축제 상태 수동 갱신",
        description = "DB에 저장된 축제의 날짜(시작일/종료일)와 오늘 날짜를 비교하여 축제 상태(예정->진행중->종료)를 일괄 업데이트합니다."
    )
    fun updateFestivalStatusManually(): RsData<Void> {
        // 이전에 FestivalSyncService에 만들어두기로 한 상태 업데이트 로직 호출
        festivalSyncService.updateFestivalStatuses()
        return RsData.success("축제 상태(진행중/종료) 수동 갱신이 완료되었습니다.")
    }
}
