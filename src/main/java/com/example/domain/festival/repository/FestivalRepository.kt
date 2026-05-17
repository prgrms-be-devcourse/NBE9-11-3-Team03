package com.example.domain.festival.repository

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface FestivalRepository : JpaRepository<Festival, Long>, FestivalRepositoryCustom {
    // 공공 API 동기화용
    fun findByContentId(contentId: String): Optional<Festival>

    //반복 단건 조회(findByContentId)로 인한 DB 병목을 줄이기 위한 메서드
    fun findAllByContentIdIn(contentIds: List<String>): List<Festival>

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.viewCount = f.viewCount + 1 WHERE f.id = :id")
    fun incrementViewCount(@Param("id") id: Long): Int

    // 축제 찜 수 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.bookMarkCount = f.bookMarkCount + 1 WHERE f.id = :festivalId")
    fun increaseBookmarkCount(@Param("festivalId") festivalId: Long)

    // 축제 찜 수 감소
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.bookMarkCount = f.bookMarkCount - 1 WHERE f.id = :festivalId AND f.bookMarkCount > 0")
    fun decreaseBookmarkCount(@Param("festivalId") festivalId: Long)

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.status = :ongoing WHERE f.status = :upcoming AND f.startDate <= :now")
    fun updateStatusToOngoing(
        @Param("ongoing") ongoing: FestivalStatus,
        @Param("upcoming") upcoming: FestivalStatus,
        @Param("now") now: LocalDateTime,
    ): Int

    // [추가] 종료일이 지난 '진행중/예정' 축제를 '종료'로 변경
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Festival f SET f.status = :ended WHERE f.status != :ended AND f.endDate < :now")
    fun updateStatusToEnded(
        @Param("ended") ended: FestivalStatus,
        @Param("now") now: LocalDateTime,
    ): Int
}
