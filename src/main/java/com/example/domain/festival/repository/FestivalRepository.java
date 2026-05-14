package com.example.domain.festival.repository;

import com.example.domain.festival.dto.FestivalSearchRequestDto;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FestivalRepository extends JpaRepository<Festival, Long>, FestivalRepositoryCustom {

    // 공공 API 동기화용
    Optional<Festival> findByContentId(String contentId);
    boolean existsByContentId(String contentId);

    // 축제 검색용
    Page<Festival> searchFestivals(FestivalSearchRequestDto searchDto, Pageable pageable);

    //반복 단건 조회(findByContentId)로 인한 DB 병목을 줄이기 위한 메서드
    List<Festival> findAllByContentIdIn(List<String> contentIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.viewCount = f.viewCount + 1 WHERE f.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // 축제 찜 수 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.bookMarkCount = f.bookMarkCount + 1 WHERE f.id = :festivalId")
    void increaseBookmarkCount(@Param("festivalId") Long festivalId);

    // 축제 찜 수 감소
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.bookMarkCount = f.bookMarkCount - 1 WHERE f.id = :festivalId AND f.bookMarkCount > 0")
    void decreaseBookmarkCount(@Param("festivalId") Long festivalId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.status = :ongoing WHERE f.status = :upcoming AND f.startDate <= :now")
    int updateStatusToOngoing(@Param("ongoing") FestivalStatus ongoing,
                              @Param("upcoming") FestivalStatus upcoming,
                              @Param("now") LocalDateTime now);

    // [추가] 종료일이 지난 '진행중/예정' 축제를 '종료'로 변경
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.status = :ended WHERE f.status != :ended AND f.endDate < :now")
    int updateStatusToEnded(@Param("ended") FestivalStatus ended,
                            @Param("now") LocalDateTime now);
}
