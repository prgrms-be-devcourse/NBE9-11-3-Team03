package com.example.domain.festival.service;

import com.example.domain.festival.dto.response.FestivalSyncStatusResponseDto;
import com.example.domain.festival.entity.DetailSyncPendingReason;
import com.example.domain.festival.entity.FestivalDetailSyncPending;
import com.example.domain.festival.repository.FestivalDetailSyncPendingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//상세 보강 재처리 대상 저장/조회/삭제를 담당하는 서비스
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalDetailSyncPendingService {

    private final FestivalDetailSyncPendingRepository pendingRepository;

    // 실패 또는 미시도 대상 contentId를 pending에 저장한다.
    public void saveOrUpdate(String contentId, DetailSyncPendingReason reason) {
        pendingRepository.findByContentId(contentId)
                .ifPresentOrElse(
                        pending -> pending.updateFailure(reason),
                        () -> pendingRepository.save(FestivalDetailSyncPending.create(contentId, reason))
                );
    }

    // 상세 보강 성공 시 pending에서 제거한다.
    public void remove(String contentId) {
        pendingRepository.deleteByContentId(contentId);
    }

    // 재처리 대상 contentId 전체 조회 (오래된 실패부터 다시 처리할 수 있도록 lastFailedAt 오름차순 기준 처리)
    @Transactional(readOnly = true)
    public List<String> findAllContentIds() {
        return pendingRepository.findAllByOrderByLastFailedAtAsc().stream()
                .map(FestivalDetailSyncPending::getContentId)
                .toList();
    }

    //재처리 대상 건수 카운터(로그용)
    @Transactional(readOnly = true)
    public long count() {
        return pendingRepository.count();
    }

    //reason별 pending 건수 조회(로그용)
    @Transactional(readOnly = true)
    public long countByReason(DetailSyncPendingReason reason) {
        return pendingRepository.countByReason(reason);
    }

    //reason별 pending 건수 조회(로그용)
    @Transactional(readOnly = true)
    public FestivalSyncStatusResponseDto getSyncStatus() {
        Map<String, Long> pendingBreakdown = new LinkedHashMap<>();

        for (DetailSyncPendingReason reason : DetailSyncPendingReason.values()) {
            pendingBreakdown.put(reason.name(), countByReason(reason));
        }

        long pendingCount = count();

        return new FestivalSyncStatusResponseDto(
                pendingCount,
                pendingBreakdown,
                pendingCount > 0
        );
    }
}
