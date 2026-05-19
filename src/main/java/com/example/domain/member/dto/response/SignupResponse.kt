package com.example.domain.member.dto.response

import com.example.domain.member.entity.Member
import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.entity.Role

data class SignupResponse(
    val memberId: Long,
    val loginId: String,
    val nickname: String,
    val role: Role,
    val status: MemberStatus
) {
    companion object {
        fun from(member: Member): SignupResponse {
            return SignupResponse(
                memberId = member.id,
                loginId = member.loginId,
                nickname = member.nickname,
                role = member.role,
                status = member.status
            )
        }
    }
}
