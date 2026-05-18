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
open class RefreshToken protected constructor() : BaseEntity() {
    // 한 회원당 refresh token 하나만 저장함.
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

    // refresh token이 재발급에 사용 가능한 상태인지 저장함.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE
        protected set

    // refresh token이 로그아웃 처리된 시간을 저장함.
    @Column(name = "logged_out_at")
    var loggedOutAt: LocalDateTime? = null
        protected set

    private constructor(member: Member, token: String, expiresAt: LocalDateTime) : this() {
        this.member = member
        this.token = token
        this.expiresAt = expiresAt
        this.status = RefreshTokenStatus.ACTIVE
    }

    // 재로그인이나 토큰 재발급 시 기존 refresh token 값을 새 값으로 교체합니다.
    fun update(token: String, expiresAt: LocalDateTime) {
        this.token = token
        this.expiresAt = expiresAt
        // 새 토큰을 저장하면 다시 사용할 수 있는 상태로 바꿈.
        status = RefreshTokenStatus.ACTIVE
        loggedOutAt = null
    }

    // 로그아웃하면 기록은 남기고 token 값과 상태만 바꿈.
    fun logout() {
        token = null
        status = RefreshTokenStatus.UNACTIVATED
        loggedOutAt = LocalDateTime.now()
    }

    // 재발급 전에 사용할 수 있는 refresh token인지 확인함.
    fun isActive(): Boolean = status == RefreshTokenStatus.ACTIVE

    fun isExpired(): Boolean = expiresAt.isBefore(LocalDateTime.now())

    companion object {
        @JvmStatic
        fun create(member: Member, token: String, expiresAt: LocalDateTime): RefreshToken =
            RefreshToken(member, token, expiresAt)
    }
}
