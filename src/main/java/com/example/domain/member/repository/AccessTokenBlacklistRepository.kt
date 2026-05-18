package com.example.domain.member.repository

import com.example.domain.member.entity.AccessTokenBlacklist
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface AccessTokenBlacklistRepository : JpaRepository<AccessTokenBlacklist, Long> {
    fun existsByToken(token: String): Boolean

    fun deleteByExpiresAtBefore(now: LocalDateTime)
}
