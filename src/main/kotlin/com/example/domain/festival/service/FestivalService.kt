package com.example.domain.festival.service

import com.example.domain.bookmark.repository.FestivalBookmarkRepository
import com.example.domain.festival.dto.request.FestivalSearchRequest
import com.example.domain.festival.dto.response.FestivalDetailResponse
import com.example.domain.festival.dto.response.FestivalListResponse
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.repository.MemberRepository
import com.example.global.exception.CustomNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class FestivalService(
    private val festivalRepository: FestivalRepository,
    private val memberRepository: MemberRepository,
    private val festivalBookmarkRepository: FestivalBookmarkRepository
) {
    fun searchFestivals(searchDto: FestivalSearchRequest, pageable: Pageable): Page<Festival> =
        festivalRepository.searchFestivals(searchDto, pageable)


    // 로그인 유저의 찜 여부까지 채워서 목록 DTO 페이지로 반환
    fun searchFestivalsDto(
        searchDto: FestivalSearchRequest,
        pageable: Pageable,
        loginId: String?
    ): Page<FestivalListResponse> {
        val page = festivalRepository.searchFestivals(searchDto, pageable)

        val bookmarkedIds = resolveBookmarkedIds(loginId, page.content)

        return page.map { festival ->
            FestivalListResponse.from(festival, festival.id in bookmarkedIds)

        }
    }

    @Transactional
    fun getFestival(id: Long): Festival {
        festivalRepository.incrementViewCount(id)

        return festivalRepository.findById(id)
            .orElseThrow { CustomNotFoundException("404", "존재하지 않는 축제입니다.") }

    }

    // 로그인 유저의 찜 여부까지 채워서 상세 DTO로 반환
    @Transactional
    fun getFestivalDetail(id: Long, loginId: String?): FestivalDetailResponse {
        val festival = getFestival(id)
        val isBookmarked = loginId
            ?.let { memberRepository.findByLoginId(it) }
            ?.let { festivalBookmarkRepository.existsByMemberIdAndFestivalId(it.id, id) }
            ?: false
        return FestivalDetailResponse.from(festival, isBookmarked)
    }

    fun getNearbyMarkers(searchDto: FestivalSearchRequest): List<Festival> =
        festivalRepository.findNearbyFestivals(searchDto)


    private fun resolveBookmarkedIds(loginId: String?, festivals: List<Festival>): Set<Long> {
        if (loginId == null || festivals.isEmpty()) return emptySet()

        val memberId = memberRepository.findByLoginId(loginId)?.id
            ?: return emptySet()

        val festivalIds = festivals.map { it.id }
        return festivalBookmarkRepository
            .findBookmarkedFestivalIds(memberId, festivalIds)
            .toSet()

    }
}
