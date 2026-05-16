package com.example.domain.bookmark.service

import com.example.domain.bookmark.dto.response.FestivalBookmarkResponse
import com.example.domain.bookmark.entity.FestivalBookmark
import com.example.domain.bookmark.repository.FestivalBookmarkRepository
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.repository.MemberRepository
import com.example.global.exception.BadRequestException
import com.example.global.exception.ConflictException
import com.example.global.exception.CustomNotFoundException
import com.example.global.exception.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

@Service
@Transactional(readOnly = true)
class FestivalBookmarkService(
    private val festivalBookmarkRepository: FestivalBookmarkRepository,
    private val festivalRepository: FestivalRepository,
    private val memberRepository: MemberRepository
) {
    @Transactional
    fun bookmarkFestival(festivalId: Long, loginId: String): FestivalBookmarkResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { UnauthorizedException("로그인이 필요한 서비스입니다.") }

        val festival = festivalRepository.findById(festivalId)
            .orElseThrow { CustomNotFoundException("해당 축제를 찾을 수 없습니다.") }

        if (festivalBookmarkRepository.existsByMemberIdAndFestivalId(member.id, festivalId)) {
            throw ConflictException("이미 찜한 축제입니다.")
        }

        // 북마크 저장 및 카운트 증가
        festivalBookmarkRepository.save(FestivalBookmark(member, festival))
        festivalRepository.increaseBookmarkCount(festivalId)

        return FestivalBookmarkResponse(
            festivalId    = festival.id,
            memberId      = member.id,
            bookmarked    = true,
            bookmarkCount = festival.bookMarkCount + 1
        )
    }

    @Transactional
    fun cancelBookmark(festivalId: Long, loginId: String): FestivalBookmarkResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { UnauthorizedException("로그인이 필요한 서비스입니다.") }

        val festival = festivalRepository.findById(festivalId)
            .orElseThrow { CustomNotFoundException("해당 축제를 찾을 수 없습니다.") }

        // 북마크 삭제 및 카운트 감소
        festivalBookmarkRepository
            .findByMemberIdAndFestivalId(member.id, festivalId)
            .orElseThrow { BadRequestException("찜하지 않은 축제입니다.") }
            .also { festivalBookmarkRepository.delete(it) }

        festivalBookmarkRepository.flush()
        festivalRepository.decreaseBookmarkCount(festivalId)

        return FestivalBookmarkResponse(
            festivalId    = festival.id,
            memberId      = member.id,
            bookmarked    = false,
            bookmarkCount = max(0, festival.bookMarkCount - 1)
        )
    }
}