package com.example.domain.member.entity

import com.example.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "access_token_blacklist")
class AccessTokenBlacklist protected constructor() : BaseEntity() {
    @Column(nullable = false, unique = true, length = 500)
    lateinit var token: String
        protected set

    @Column(name = "expires_at", nullable = false)
    lateinit var expiresAt: LocalDateTime
        protected set

    companion object {
        fun create(token: String, expiresAt: LocalDateTime): AccessTokenBlacklist =
            AccessTokenBlacklist().apply {
                this.token = token
                this.expiresAt = expiresAt
            }
    }
}
