package com.example.domain.member.dto.response

import com.example.domain.member.entity.Member
import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.entity.Role
import com.fasterxml.jackson.annotation.JsonIgnore

data class LoginResponse(
    val accessToken: String,
    @get:JsonIgnore
    val refreshToken: String,
    val memberId: Long,
    val loginId: String,
    val nickname: String,
    val role: Role,
    val status: MemberStatus
) {
    companion object {
        @JvmStatic
        fun of(accessToken: String, refreshToken: String, member: Member): LoginResponse {
            return LoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                memberId = member.id,
                loginId = member.loginId,
                nickname = member.nickname,
                role = member.role,
                status = member.status
            )
        }
    }
}
