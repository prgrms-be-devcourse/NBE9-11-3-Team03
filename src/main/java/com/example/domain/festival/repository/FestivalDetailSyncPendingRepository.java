package com.example.domain.festival.repository;

import com.example.domain.festival.entity.DetailSyncPendingReason;
import com.example.domain.festival.entity.FestivalDetailSyncPending;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


//상세 보강 재처리 대상 조회/저장/삭제용 Repository
public interface FestivalDetailSyncPendingRepository extends JpaRepository<FestivalDetailSyncPending, Long> {

    Optional<FestivalDetailSyncPending> findByContentId(String contentId);

    boolean existsByContentId(String contentId);

    List<FestivalDetailSyncPending> findAllByOrderByLastFailedAtAsc();

    void deleteByContentId(String contentId);

    long countByReason(DetailSyncPendingReason reason);
}
