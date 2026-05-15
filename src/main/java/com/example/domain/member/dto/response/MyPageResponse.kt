package com.example.domain.member.dto.response

import com.example.domain.member.entity.Role


data class MyPageResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val reviewCount: Long,
    val bookMarkCount: Long,
    val role: Role
)
