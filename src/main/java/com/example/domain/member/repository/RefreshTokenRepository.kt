package com.example.domain.member.repository

import com.example.domain.member.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?

    fun findByMemberId(memberId: Long): RefreshToken?
}
