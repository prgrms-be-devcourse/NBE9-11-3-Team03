package com.example.domain.member.entity

import com.example.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_token")
class RefreshToken protected constructor() : BaseEntity() {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    lateinit var member: Member
        protected set

    @Column(nullable = true, unique = true, length = 500)
    var token: String? = null
        protected set

    @Column(name = "expires_at", nullable = false)
    lateinit var expiresAt: LocalDateTime
        protected set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE
        protected set

    @Column(name = "logged_out_at")
    var loggedOutAt: LocalDateTime? = null
        protected set

    fun update(token: String, expiresAt: LocalDateTime) {
        this.token = token
        this.expiresAt = expiresAt
        status = RefreshTokenStatus.ACTIVE
        loggedOutAt = null
    }

    fun logout() {
        token = null
        status = RefreshTokenStatus.UNACTIVATED
        loggedOutAt = LocalDateTime.now()
    }

    fun isActive(): Boolean = status == RefreshTokenStatus.ACTIVE

    fun isExpired(): Boolean = expiresAt.isBefore(LocalDateTime.now())

    companion object {
        fun create(member: Member, token: String, expiresAt: LocalDateTime): RefreshToken =
            RefreshToken().apply {
                this.member = member
                this.token = token
                this.expiresAt = expiresAt
            }
    }
}
