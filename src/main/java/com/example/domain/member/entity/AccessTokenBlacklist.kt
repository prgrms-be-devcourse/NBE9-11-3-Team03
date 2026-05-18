package com.example.domain.member.entity

import com.example.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "access_token_blacklist")
open class AccessTokenBlacklist protected constructor() : BaseEntity() {
    // 로그아웃된 access token을 저장함.
    @Column(nullable = false, unique = true, length = 500)
    lateinit var token: String
        protected set

    // 이 시간이 지나면 blacklist 기록을 지워도 됨.
    @Column(name = "expires_at", nullable = false)
    lateinit var expiresAt: LocalDateTime
        protected set

    private constructor(token: String, expiresAt: LocalDateTime) : this() {
        this.token = token
        this.expiresAt = expiresAt
    }

    companion object {
        @JvmStatic
        fun create(token: String, expiresAt: LocalDateTime): AccessTokenBlacklist =
            AccessTokenBlacklist(token, expiresAt)
    }
}
