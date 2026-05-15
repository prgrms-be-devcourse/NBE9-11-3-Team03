package com.example.domain.member.dto.response

import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.entity.Role

data class SignupResponse(
    val memberId: Long?,
    val loginId: String?,
    val nickname: String?,
    val role: Role?,
    val status: MemberStatus?,
)
