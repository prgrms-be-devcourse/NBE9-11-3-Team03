package com.example.domain.member.dto.response

import com.example.domain.member.entity.MemberStatus

data class WithdrawResponse(
    val memberId: Long?,
    val status: MemberStatus,
)
