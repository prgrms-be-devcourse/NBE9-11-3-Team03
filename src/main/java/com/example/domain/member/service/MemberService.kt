package com.example.domain.member.service

import com.example.domain.admin.dto.response.AdminMemberWithdrawnResponse
import com.example.domain.admin.dto.response.MemberPageResponse
import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.repository.MemberRepository
import com.example.domain.member.repository.RefreshTokenRepository
import com.example.global.exception.CustomNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    fun getAllMembers(pageable: Pageable): MemberPageResponse {
        return MemberPageResponse.from(memberRepository.findAll(pageable))
    }

    fun getReportMembers(pageable: Pageable): MemberPageResponse {
        val memberPage = memberRepository.findAllByReportCountGreaterThanEqualAndStatus(
            5,
            MemberStatus.ACTIVE,
            pageable
        )

        return MemberPageResponse.from(memberPage)
    }

    @Transactional
    fun memberWithdraw(memberId: Long): AdminMemberWithdrawnResponse {
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw CustomNotFoundException("404", "존재하지 않는 회원입니다.")

        if (member.status == MemberStatus.WITHDRAWN) {
            log.warn("[ADMIN] 회원 강제 탈퇴 실패(이미 탈퇴한 회원) - memberId={}", memberId)
            throw IllegalArgumentException("이미 탈퇴 처리된 회원입니다.")
        }

        member.withdraw()
        refreshTokenRepository.findByMemberId(member.id)?.logout()

        return AdminMemberWithdrawnResponse.from(member)
    }

    companion object {
        private val log = LoggerFactory.getLogger(MemberService::class.java)
    }
}
