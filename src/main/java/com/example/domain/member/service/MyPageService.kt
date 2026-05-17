package com.example.domain.member.service

import com.example.domain.bookmark.repository.FestivalBookmarkRepository
import com.example.domain.member.dto.response.MyBookMarkPageResponse
import com.example.domain.member.dto.response.MyPageResponse
import com.example.domain.member.dto.response.MyReviewPageResponse
import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.example.global.exception.CustomNotFoundException
import com.example.global.exception.ForbiddenException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MyPageService(
    private val memberRepository: MemberRepository,
    private val reviewRepository: ReviewRepository,
    private val festivalBookmarkRepository: FestivalBookmarkRepository
) {

    private fun findMemberByLoginId(loginId: String) =
        memberRepository.findByLoginId(loginId)
            .orElseThrow { CustomNotFoundException("로그인한 회원 정보를 찾을 수 없습니다.") }

    //마이페이지의 자신을 조회하는 메서드입니다.
    fun getMyPage(loginId: String): MyPageResponse {
        val member = findMemberByLoginId(loginId).also {
            if (it.status == MemberStatus.WITHDRAWN)
                throw ForbiddenException("탈퇴한 회원은 마이페이지를 조회할 수 없습니다.")
        }

        return MyPageResponse(
            memberId = member.id,
            email = member.email,
            nickname = member.nickname,
            //자신이 단 리뷰수
            reviewCount = reviewRepository.countByMemberIdAndStatus(member.id, ReviewStatus.ACTIVE),
            //자신이 찜한 축제의수
            bookMarkCount = festivalBookmarkRepository.countByMemberId(member.id),
            role = member.role
        )
    }

    //loginId를 토대로 내가 쓴 리뷰를 찾고, 그 리뷰를 페이징하여 넘겨주는 메서드
    fun getMyReviews(loginId: String, pageable: Pageable): MyReviewPageResponse =
        reviewRepository
            .findByMemberIdAndStatus(findMemberByLoginId(loginId).id, ReviewStatus.ACTIVE, pageable)
            .let { MyReviewPageResponse.from(it) }

    // loginId로 자신이 찜한 축제를 찾아 반환하는 메서드
    fun getMyBookMark(loginId: String, pageable: Pageable): MyBookMarkPageResponse =
        festivalBookmarkRepository
            .findByMemberId(findMemberByLoginId(loginId).id, pageable)
            .let { MyBookMarkPageResponse.from(it) }
}