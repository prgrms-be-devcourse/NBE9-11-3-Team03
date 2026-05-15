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
import java.util.function.Supplier

@Service
@Transactional(readOnly = true)
class MyPageService(
    private val memberRepository: MemberRepository,
    private val reviewRepository: ReviewRepository,
    private val festivalBookmarkRepository: FestivalBookmarkRepository
) {
    //마이페이지의 자신을 조회하는 메서드입니다.
    fun getMyPage(loginId: String): MyPageResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { CustomNotFoundException("로그인한 회원 정보를 찾을 수 없습니다.") }

        if (member.status == MemberStatus.WITHDRAWN) {
            throw ForbiddenException("탈퇴한 회원은 마이페이지를 조회할 수 없습니다.")
        }
        //자신이 단 리뷰수
        val reviewCount = reviewRepository.countByMemberIdAndStatus(member.id, ReviewStatus.ACTIVE)
        //자신이 찜한 축제의수
        val bookMarkCount = festivalBookmarkRepository.countByMemberId(member.id)

        return MyPageResponse(
            memberId = member.id,
            email = member.email,
            nickname = member.nickname,
            reviewCount = reviewCount,
            bookMarkCount = bookMarkCount,
            role = member.role
        )
    }

    //logind를 토대로 내가 쓴 리뷰를 찾고, 그리뷰를 페이징하여 넘겨주는 메서드
    fun getMyReviews(loginId: String, pageable: Pageable): MyReviewPageResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { CustomNotFoundException("로그인한 회원 정보를 찾을 수 없습니다.") }

        return reviewRepository
            .findByMemberIdAndStatus(member.id, ReviewStatus.ACTIVE, pageable)
            .let { MyReviewPageResponse.from(it) }
    }

    // loginId로 자신이 찜한 축제를 찾아 반환하는 메서드
    fun getMyBookMark(loginId: String, pageable: Pageable): MyBookMarkPageResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { CustomNotFoundException("로그인한 회원 정보를 찾을 수 없습니다.") }

        return festivalBookmarkRepository
            .findByMemberId(member.id, pageable)
            .let { MyBookMarkPageResponse.from(it) }
    }
}
