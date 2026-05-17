package com.example.domain.member.repository

import com.example.domain.member.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String?): Optional<RefreshToken>

    fun findByMemberId(memberId: Long?): Optional<RefreshToken>
}
