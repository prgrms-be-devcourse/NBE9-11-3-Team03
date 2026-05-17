package com.example.domain.bookmark.repository

import com.example.domain.bookmark.entity.FestivalBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface FestivalBookmarkRepository : JpaRepository<FestivalBookmark, Long> {
    fun existsByMemberIdAndFestivalId(memberId: Long, festivalId: Long): Boolean

    fun countByMemberId(memberId: Long): Long

    @Query("SELECT fb FROM FestivalBookmark fb JOIN FETCH fb.festival WHERE fb.member.id = :memberId")
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<FestivalBookmark>

    fun findByMemberIdAndFestivalId(memberId: Long, festivalId: Long): FestivalBookmark?

    @Query(
        "SELECT fb.festival.id FROM FestivalBookmark fb " +
                "WHERE fb.member.id = :memberId AND fb.festival.id IN :festivalIds"
    )
    fun findBookmarkedFestivalIds(
        @Param("memberId") memberId: Long,
        @Param("festivalIds") festivalIds: Collection<Long>
    ): List<Long>
}